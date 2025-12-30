/**
 * @swagger
 * tags:
 *   - name: Spotify
 *     description: "API tương tác với Spotify"
 * 
 * components:
 *   schemas:
 *     SpotifyArtist:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           example: "7JMwQ9JGjI6A8Q1Zn2SSak"
 *         name:
 *           type: string
 *           example: "Sơn Tùng M-TP"
 *         followers:
 *           type: number
 *           example: 1000000
 *         genres:
 *           type: array
 *           items:
 *             type: string
 *           example: 
 *             - "v-pop"
 *             - "vietnamese pop"
 *         images:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/SpotifyImage'
 *         popularity:
 *           type: number
 *           example: 85

 *     SpotifyImage:
 *       type: object
 *       properties:
 *         url:
 *           type: string
 *           format: uri
 *           example: "https://i.scdn.co/image/..."
 *         width:
 *           type: number
 *           example: 640
 *         height:
 *           type: number
 *           example: 640
 * 
 *     SpotifyTrack:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           example: "4uLU6hMCjMI75M1A2tKUQC"
 *         name:
 *           type: string
 *           example: "Hãy Trao Cho Anh"
 *         duration_ms:
 *           type: number
 *           example: 229560
 *         preview_url:
 *           type: string
 *           format: uri
 *           example: "https://p.scdn.co/mp3-preview/..."
 *         album:
 *           $ref: '#/components/schemas/SpotifyAlbum'
 *         artists:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/SpotifyArtistSimple'
 * 
 *     SpotifyAlbum:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           example: "1zG1VuUjHsPGUX5PhjhqhN"
 *         name:
 *           type: string
 *           example: "Chúng Ta Của Hiện Tại"
 *         images:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/SpotifyImage'
 * 
 *     SpotifyArtistSimple:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           example: "7JMwQ9JGjI6A8Q1Zn2SSak"
 *         name:
 *           type: string
 *           example: "Sơn Tùng M-TP"
 * 
 *     SearchResultTrack:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           example: "4uLU6hMCjMI75M1A2tKUQC"
 *         name:
 *           type: string
 *           example: "Hãy Trao Cho Anh"
 *         artists:
 *           type: string
 *           example: "Sơn Tùng M-TP"
 *         album:
 *           type: string
 *           example: "Chúng Ta Của Hiện Tại"
 *         preview_url:
 *           type: string
 *           format: uri
 *           example: "https://p.scdn.co/mp3-preview/..."
 *         external_url:
 *           type: string
 *           format: uri
 *           example: "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC"
 */

/**
 * @swagger
 * /spotify/artists/{id}:
 *   get:
 *     tags: [Spotify]
 *     summary: "Lấy thông tin chi tiết nghệ sĩ"
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: "ID của nghệ sĩ trên Spotify"
 *     responses:
 *       '200':
 *         description: "Thành công"
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   $ref: '#/components/schemas/SpotifyArtist'
 *       '404':
 *         description: "Không tìm thấy nghệ sĩ"
 *       '500':
 *         description: "Lỗi server"
 */

/**
 * @swagger
 * /spotify/artists/{id}/top-tracks:
 *   get:
 *     tags: [Spotify]
 *     summary: "Lấy danh sách bài hát phổ biến của nghệ sĩ"
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: "ID của nghệ sĩ trên Spotify"
 *       - in: query
 *         name: market
 *         schema:
 *           type: string
 *           default: "US"
 *         description: "Mã quốc gia (VD: VN, US, JP). Mặc định là US"
 *     responses:
 *       '200':
 *         description: "Thành công"
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 market:
 *                   type: string
 *                   example: "US"
 *                 data:
 *                   type: array
 *                   items:
 *                     $ref: '#/components/schemas/SpotifyTrack'
 *       '404':
 *         description: "Không tìm thấy bài hát nào cho nghệ sĩ này"
 *       '500':
 *         description: "Lỗi server"
 */

/**
 * @swagger
 * /spotify/search:
 *   get:
 *     tags: [Spotify]
 *     summary: "Tìm kiếm bài hát trên Spotify"
 *     parameters:
 *       - in: query
 *         name: q
 *         required: true
 *         schema:
 *           type: string
 *         description: "Từ khóa tìm kiếm"
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           default: "track"
 *         description: "Loại tìm kiếm (mặc định: track)"
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 10
 *         description: "Số lượng kết quả trả về (tối đa 50)"
 *     responses:
 *       '200':
 *         description: "Thành công"
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   type: array
 *                   items:
 *                     $ref: '#/components/schemas/SearchResultTrack'
 *       '400':
 *         description: "Thiếu từ khóa tìm kiếm"
 *       '500':
 *         description: "Lỗi server"
 */