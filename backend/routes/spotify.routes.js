const express = require('express');
const router = express.Router();
const axios = require('axios');
const { spotifyApi, getAccessToken } = require('../config/spotify');

// Middleware để đảm bảo có token hợp lệ
const ensureAuth = async (req, res, next) => {
  // /search đã chuyển sang Deezer -> không cần token Spotify
  if (req.path === '/search') return next();
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

// Tìm kiếm bài hát.
// LƯU Ý: Spotify chặn request từ IP nhà cung cấp cloud (Render) -> 403 body rỗng dù token hợp lệ.
// Nên dùng Deezer (API mở, không cần token, không chặn IP). Giữ NGUYÊN format trả về để app không phải sửa.
router.get('/search', async (req, res) => {
  try {
    const { q, limit = 10 } = req.query;

    if (!q) {
      return res.status(400).json({
        success: false,
        error: 'Vui lòng nhập từ khóa tìm kiếm'
      });
    }

    const dz = await axios.get('https://api.deezer.com/search', {
      params: { q, limit: parseInt(limit, 10) || 10 },
      timeout: 10000
    });

    const tracks = Array.isArray(dz.data && dz.data.data) ? dz.data.data : [];
    const data = tracks.map(track => ({
      id: String(track.id),
      name: track.title,
      artists: track.artist && track.artist.name ? track.artist.name : '',
      album: track.album && track.album.title ? track.album.title : '',
      preview_url: track.preview || null,
      external_url: track.link || null,
      cover_url:
        (track.album && (track.album.cover_xl || track.album.cover_big || track.album.cover_medium)) || null
    }));

    res.json({ success: true, data });
  } catch (error) {
    console.error('Lỗi khi tìm nhạc (Deezer):', error.message);
    res.status(error.response?.status || 500).json({
      success: false,
      error: 'Không tìm được nhạc, vui lòng thử lại'
    });
  }
});

module.exports = router;