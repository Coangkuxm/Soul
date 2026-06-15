const express = require('express');
const router = express.Router();
const { spotifyApi, getAccessToken } = require('../config/spotify');

// Middleware để đảm bảo có token hợp lệ
const ensureAuth = async (req, res, next) => {
  try {
    if (!spotifyApi.getAccessToken()) {
      const ok = await getAccessToken();
      if (!ok) {
        return res.status(503).json({
          success: false,
          error: 'Spotify chưa sẵn sàng: lấy access token thất bại (kiểm tra SPOTIFY_CLIENT_ID/SECRET trên server).'
        });
      }
    }
    next();
  } catch (error) {
    console.error('Lỗi xác thực:', error);
    return res.status(401).json({
      success: false,
      error: 'Lỗi xác thực với Spotify'
    });
  }
};

// Áp dụng middleware cho tất cả các route
router.use(ensureAuth);

// Lấy thông tin nghệ sĩ
router.get('/artists/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { body } = await spotifyApi.getArtist(id);
    
    res.json({
      success: true,
      data: {
        id: body.id,
        name: body.name,
        followers: body.followers?.total || 0,
        genres: body.genres || [],
        images: body.images || [],
        popularity: body.popularity || 0
      }
    });
  } catch (error) {
    console.error('Lỗi khi lấy thông tin nghệ sĩ:', error);
    res.status(error.statusCode || 500).json({
      success: false,
      error: error.message || 'Có lỗi xảy ra'
    });
  }
});

// Lấy top tracks của nghệ sĩ
router.get('/artists/:id/top-tracks', async (req, res) => {
  try {
    const { id } = req.params;
    const { market = 'US' } = req.query;  // Mặc định là US nếu không có market
    
    // Quan trọng: Phải truyền market dưới dạng object { market: 'US' }
    const { body } = await spotifyApi.getArtistTopTracks(id, { market });
    
    if (!body.tracks) {
      return res.status(404).json({
        success: false,
        error: 'Không tìm thấy bài hát nào cho nghệ sĩ này'
      });
    }

    res.json({
      success: true,
      data: body.tracks.map(track => ({
        id: track.id,
        name: track.name,
        duration_ms: track.duration_ms,
        preview_url: track.preview_url,
        album: {
          id: track.album.id,
          name: track.album.name,
          images: track.album.images
        },
        artists: track.artists.map(artist => ({
          id: artist.id,
          name: artist.name
        }))
      }))
    });
  } catch (error) {
    console.error('Lỗi khi lấy top tracks:', error);
    
    // Log chi tiết lỗi để debug
    console.log('Status code:', error.statusCode);
    console.log('Error body:', error.body);
    
    res.status(error.statusCode || 500).json({
      success: false,
      error: error.message || 'Có lỗi xảy ra',
      // Chỉ hiển thị chi tiết lỗi trong môi trường development
      ...(process.env.NODE_ENV === 'development' && {
        details: {
          statusCode: error.statusCode,
          body: error.body
        }
      })
    });
  }
});

// Tìm kiếm bài hát
router.get('/search', async (req, res) => {
  try {
    const { q, type = 'track', limit = 10 } = req.query;
    
    if (!q) {
      return res.status(400).json({
        success: false,
        error: 'Vui lòng nhập từ khóa tìm kiếm'
      });
    }

    // Luôn đảm bảo có token mới trước khi search (token có thể hết hạn)
    if (!spotifyApi.getAccessToken()) {
      const ok = await getAccessToken();
      if (!ok) {
        return res.status(503).json({
          success: false,
          error: 'Không lấy được Spotify access token (kiểm tra SPOTIFY_CLIENT_ID/SECRET).',
          diag: { reason: 'token_grant_failed' }
        });
      }
    }

    const { body } = await spotifyApi.searchTracks(q, { limit: parseInt(limit, 10) || 10 });

    res.json({
      success: true,
      data: body.tracks.items.map(track => ({
        id: track.id,
        name: track.name,
        artists: track.artists.map(a => a.name).join(', '),
        album: track.album.name,
        preview_url: track.preview_url,
        external_url: track.external_urls.spotify,
        cover_url: track.album.images?.[0]?.url || null // Lấy ảnh bìa album (size lớn nhất)
      }))
    });
  } catch (error) {
    console.error('Lỗi khi tìm kiếm Spotify:', {
      statusCode: error.statusCode,
      message: error.message,
      body: error.body
    });
    // spotify-web-api-node để thông tin hữu ích trong error.body, tránh trả "[object Object]"
    const spotifyMsg =
      error?.body?.error?.message ||
      (typeof error?.body?.error === 'string' ? error.body.error : null) ||
      'Spotify search failed';
    res.status(error.statusCode || 500).json({
      success: false,
      error: spotifyMsg,
      // TẠM THỜI để chẩn đoán — sẽ gỡ sau khi xác định nguyên nhân
      diag: {
        statusCode: error.statusCode || null,
        body: error.body || null,
        hasToken: Boolean(spotifyApi.getAccessToken()),
        rawMessage: typeof error?.message === 'string' ? error.message : String(error?.message)
      }
    });
  }
});

module.exports = router;