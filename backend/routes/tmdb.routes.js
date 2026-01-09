const express = require('express');
const router = express.Router();
const tmdbController = require('../controllers/tmdb.controller');
const rateLimit = require('../middlewares/rateLimit');

// Public routes
router.get('/search', rateLimit.tmdbRateLimiter, tmdbController.search);
router.get('/movies/popular', rateLimit.tmdbRateLimiter, tmdbController.getPopularMovies);
router.get('/movies/:id', rateLimit.tmdbRateLimiter, tmdbController.getMovieDetails);
router.get('/tv/:id', rateLimit.tmdbRateLimiter, tmdbController.getTvShowDetails);

module.exports = router;