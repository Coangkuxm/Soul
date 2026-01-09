// services/tmdb.service.js
const axios = require('axios');
const { TMDB_API_KEY, TMDB_ACCESS_TOKEN, TMDB_BASE_URL, TMDB_IMAGE_BASE_URL } = process.env;

// Default image base URL if not provided in env
const IMAGE_BASE_URL = TMDB_IMAGE_BASE_URL || 'https://image.tmdb.org/t/p';

// Helper function to get full image URL
const getImageUrl = (path, size = 'w500') => {
  if (!path) return null;
  return `${IMAGE_BASE_URL}/${size}${path}`;
};

// Helper to process movie/TV show data
const processMediaData = (data) => {
  if (!data) return null;
  
  return {
    ...data,
    poster_path: getImageUrl(data.poster_path),
    backdrop_path: getImageUrl(data.backdrop_path, 'original'),
    // Add more image URLs in different sizes if needed
    images: {
      poster: {
        w92: getImageUrl(data.poster_path, 'w92'),
        w154: getImageUrl(data.poster_path, 'w154'),
        w342: getImageUrl(data.poster_path, 'w342'),
        w500: getImageUrl(data.poster_path, 'w500'),
        original: getImageUrl(data.poster_path, 'original')
      },
      backdrop: {
        w300: getImageUrl(data.backdrop_path, 'w300'),
        w780: getImageUrl(data.backdrop_path, 'w780'),
        w1280: getImageUrl(data.backdrop_path, 'w1280'),
        original: getImageUrl(data.backdrop_path, 'original')
      }
    }
  };
};

const tmdbApi = axios.create({
  baseURL: TMDB_BASE_URL || 'https://api.themoviedb.org/3',
  headers: {
    'Authorization': `Bearer ${TMDB_ACCESS_TOKEN}`,
    'Content-Type': 'application/json'
  },
  params: {
    api_key: TMDB_API_KEY,
    language: 'vi-VN'
  }
});

class TMDBService {
  static async search(query, page = 1) {
    try {
      const response = await tmdbApi.get('/search/multi', {
        params: { 
          query, 
          page,
          include_adult: false
        }
      });
      
      // Process results to include full image URLs
      const processedResults = {
        ...response.data,
        results: response.data.results.map(item => processMediaData(item))
      };
      
      return processedResults;
    } catch (error) {
      console.error('TMDB Search Error:', error.response?.data || error.message);
      throw error;
    }
  }

  static async getMovieDetails(movieId) {
    try {
      const response = await tmdbApi.get(`/movie/${movieId}`, {
        params: {
          append_to_response: 'videos,credits,images,similar,recommendations',
          language: 'vi-VN',
          include_image_language: 'vi,null,en'
        }
      });
      return processMediaData(response.data);
    } catch (error) {
      console.error('TMDB Movie Details Error:', error.response?.data || error.message);
      throw error;
    }
  }

  static async getPopularMovies(page = 1) {
    try {
      const response = await tmdbApi.get('/movie/popular', { 
        params: { 
          page,
          language: 'vi-VN',
          region: 'VN'
        } 
      });
      
      // Process results to include full image URLs
      return {
        ...response.data,
        results: response.data.results.map(movie => processMediaData(movie))
      };
    } catch (error) {
      console.error('TMDB Popular Movies Error:', error.response?.data || error.message);
      throw error;
    }
  }

  static async getTvShowDetails(tvId) {
    try {
      const response = await tmdbApi.get(`/tv/${tvId}`, {
        params: {
          append_to_response: 'videos,credits,images,similar,recommendations'
        }
      });
      return response.data;
    } catch (error) {
      console.error('TMDB TV Show Error:', error.response?.data || error.message);
      throw error;
    }
  }
}

module.exports = TMDBService;