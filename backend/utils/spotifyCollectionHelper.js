const { spotifyApi, getAccessToken } = require('../config/spotify');

/**
 * Fetches Spotify track details by ID
 * @param {string} trackId - Spotify track ID
 * @returns {Promise<Object>} Track details
 */
const getSpotifyTrack = async (trackId) => {
    try {
        await getAccessToken();
        const { body: track } = await spotifyApi.getTrack(trackId);
        
        return {
            id: track.id,
            name: track.name,
            type: 'track',
            artists: track.artists.map(artist => ({
                id: artist.id,
                name: artist.name
            })),
            album: {
                id: track.album.id,
                name: track.album.name,
                images: track.album.images,
                release_date: track.album.release_date
            },
            duration_ms: track.duration_ms,
            preview_url: track.preview_url,
            external_url: track.external_urls.spotify,
            popularity: track.popularity
        };
    } catch (error) {
        console.error('Error fetching Spotify track:', error);
        throw new Error('Failed to fetch track from Spotify');
    }
};

/**
 * Fetches Spotify album details by ID
 * @param {string} albumId - Spotify album ID
 * @returns {Promise<Object>} Album details with tracks
 */
const getSpotifyAlbum = async (albumId) => {
    try {
        await getAccessToken();
        const { body: album } = await spotifyApi.getAlbum(albumId);
        const { body: { items: tracks } } = await spotifyApi.getAlbumTracks(albumId);
        
        return {
            id: album.id,
            name: album.name,
            type: 'album',
            artists: album.artists.map(artist => ({
                id: artist.id,
                name: artist.name
            })),
            images: album.images,
            release_date: album.release_date,
            total_tracks: album.total_tracks,
            tracks: tracks.map(track => ({
                id: track.id,
                name: track.name,
                duration_ms: track.duration_ms,
                track_number: track.track_number,
                preview_url: track.preview_url,
                external_url: track.external_urls.spotify
            })),
            external_url: album.external_urls.spotify
        };
    } catch (error) {
        console.error('Error fetching Spotify album:', error);
        throw new Error('Failed to fetch album from Spotify');
    }
};

/**
 * Enriches collection items with Spotify data
 * @param {Array} items - Array of collection items
 * @returns {Promise<Array>} Enriched collection items
 */
const enrichCollectionItems = async (items) => {
    if (!items || !items.length) return items;
    
    const enrichedItems = [];
    
    for (const item of items) {
        try {
            if (item.spotify_id && item.spotify_type === 'track') {
                const spotifyData = await getSpotifyTrack(item.spotify_id);
                enrichedItems.push({
                    ...item,
                    spotify_data: spotifyData
                });
            } else if (item.spotify_id && item.spotify_type === 'album') {
                const spotifyData = await getSpotifyAlbum(item.spotify_id);
                enrichedItems.push({
                    ...item,
                    spotify_data: spotifyData
                });
            } else {
                enrichedItems.push(item);
            }
        } catch (error) {
            console.error(`Error enriching item ${item.id}:`, error);
            // Return the item without enrichment if there's an error
            enrichedItems.push(item);
        }
    }
    
    return enrichedItems;
};

/**
 * Extracts Spotify ID and type from a Spotify URL
 * @param {string} url - Spotify URL
 * @returns {{id: string, type: string}|null} Spotify ID and type, or null if invalid
 */
const parseSpotifyUrl = (url) => {
    if (!url) return null;
    
    // Handle different Spotify URL formats
    const trackMatch = url.match(/spotify:track:([a-zA-Z0-9]+)/) || 
                      url.match(/open\.spotify\.com\/track\/([a-zA-Z0-9]+)/);
    if (trackMatch) {
        return { id: trackMatch[1], type: 'track' };
    }
    
    const albumMatch = url.match(/spotify:album:([a-zA-Z0-9]+)/) || 
                      url.match(/open\.spotify\.com\/album\/([a-zA-Z0-9]+)/);
    if (albumMatch) {
        return { id: albumMatch[1], type: 'album' };
    }
    
    return null;
};

module.exports = {
    getSpotifyTrack,
    getSpotifyAlbum,
    enrichCollectionItems,
    parseSpotifyUrl
};
