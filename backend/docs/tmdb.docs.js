/**
 * @swagger
 * tags:
 *   name: TMDB
 *   description: The TMDB API endpoints
 */

/**
 * @swagger
 * /tmdb/search:
 *   get:
 *     summary: Search for movies and TV shows
 *     tags: [TMDB]
 *     parameters:
 *       - in: query
 *         name: query
 *         schema:
 *           type: string
 *         required: true
 *         description: Search query
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *     responses:
 *       200:
 *         description: Search results
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   $ref: '#/components/schemas/TMDBResults'
 */

/**
 * @swagger
 * /tmdb/movies/popular:
 *   get:
 *     summary: Get popular movies
 *     tags: [TMDB]
 *     parameters:
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *     responses:
 *       200:
 *         description: List of popular movies
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   $ref: '#/components/schemas/TMDBResults'
 */

/**
 * @swagger
 * /tmdb/movies/{id}:
 *   get:
 *     summary: Get movie details by ID
 *     tags: [TMDB]
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *     responses:
 *       200:
 *         description: Movie details
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   $ref: '#/components/schemas/TMDBMovie'
 */

/**
 * @swagger
 * /tmdb/tv/{id}:
 *   get:
 *     summary: Get TV show details by ID
 *     tags: [TMDB]
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *     responses:
 *       200:
 *         description: TV show details
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   $ref: '#/components/schemas/TMDBTVShow'
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     TMDBResults:
 *       type: object
 *       properties:
 *         page:
 *           type: integer
 *         results:
 *           type: array
 *           items:
 *             oneOf:
 *               - $ref: '#/components/schemas/TMDBMovie'
 *               - $ref: '#/components/schemas/TMDBTVShow'
 *         total_pages:
 *           type: integer
 *         total_results:
 *           type: integer
 * 
 *     TMDBMovie:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         title:
 *           type: string
 *         overview:
 *           type: string
 *         poster_path:
 *           type: string
 *           nullable: true
 *         backdrop_path:
 *           type: string
 *           nullable: true
 *         release_date:
 *           type: string
 *           format: date
 *         vote_average:
 *           type: number
 *         vote_count:
 *           type: integer
 *         genre_ids:
 *           type: array
 *           items:
 *             type: integer
 * 
 *     TMDBTVShow:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         name:
 *           type: string
 *         overview:
 *           type: string
 *         poster_path:
 *           type: string
 *           nullable: true
 *         backdrop_path:
 *           type: string
 *           nullable: true
 *         first_air_date:
 *           type: string
 *           format: date
 *         vote_average:
 *           type: number
 *         vote_count:
 *           type: integer
 *         genre_ids:
 *           type: array
 *           items:
 *             type: integer
 */
