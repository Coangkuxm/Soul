const TMDBService = require('../services/tmdb.service');
const { BadRequestError } = require('../utils/errors');

const tmdbController = {
  // Search for movies/TV shows
  async search(req, res, next) {
    try {
      const { query, page = 1 } = req.query;
      
      if (!query) {
        throw new BadRequestError('Vui lòng nhập từ khóa tìm kiếm');
      }

      const results = await TMDBService.search(query, page);
      res.json({
        success: true,
        data: results
      });
    } catch (error) {
      next(error);
    }
  },

  // Get movie details
  async getMovieDetails(req, res, next) {
    try {
      const { id } = req.params;
      const movie = await TMDBService.getMovieDetails(id);
      
      res.json({
        success: true,
        data: movie
      });
    } catch (error) {
      next(error);
    }
  },

  // Get TV show details
  async getTvShowDetails(req, res, next) {
    try {
      const { id } = req.params;
      const show = await TMDBService.getTvShowDetails(id);
      
      res.json({
        success: true,
        data: show
      });
    } catch (error) {
      next(error);
    }
  },

  // Get popular movies
  async getPopularMovies(req, res, next) {
    try {
      const { page = 1 } = req.query;
      const movies = await TMDBService.getPopularMovies(page);
      
      res.json({
        success: true,
        data: movies
      });
    } catch (error) {
      next(error);
    }
  }
};

module.exports = tmdbController;