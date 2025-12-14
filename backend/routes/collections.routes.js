const express = require('express');
const router = express.Router();
const { body, param, query } = require('express-validator');
const collectionsController = require('../controllers/collections.controller');
const { authenticateJWT } = require('../middleware/auth.middleware');
const { validate } = require('../middleware/validation.middleware');

/**
 * @swagger
 * tags:
 *   name: Collections
 *   description: Quản lý bộ sưu tập
 */

// Tạo bộ sưu tập mới
/**
 * @swagger
 * /collections:
 *   post:
 *     summary: Tạo bộ sưu tập mới
 *     tags: [Collections]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - name
 *             properties:
 *               name:
 *                 type: string
 *                 example: "Bộ sưu tập mùa hè"
 *               description:
 *                 type: string
 *                 example: "Những sản phẩm tốt nhất mùa hè 2025"
 *               cover_image_url:
 *                 type: string
 *                 format: uri
 *                 example: "https://example.com/images/summer-collection.jpg"
 *               is_private:
 *                 type: boolean
 *                 default: false
 *               tags:
 *                 type: array
 *                 items:
 *                   type: string
 *                 example: ["mùa hè", "thời trang", "phụ kiện"]
 *     responses:
 *       201:
 *         description: Tạo bộ sưu tập thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   $ref: '#/components/schemas/Collection'
 *       400:
 *         description: Lỗi validate dữ liệu
 *       401:
 *         description: Chưa đăng nhập
 */
router.post(
  '/',
  authenticateJWT,
  [
    body('name').trim().notEmpty().withMessage('Tên không được để trống'),
    body('description').optional().trim(),
    body('cover_image_url').optional().isURL().withMessage('URL không hợp lệ'),
    body('is_private').optional().isBoolean().withMessage('Giá trị phải là true hoặc false'),
    body('tags').optional().isArray().withMessage('Tags phải là một mảng'),
    body('tags.*').isString().withMessage('Mỗi tag phải là chuỗi'),
  ],
  validate,
  collectionsController.createCollection
);

/**
 * @swagger
 * /collections:
 *   get:
 *     summary: Lấy danh sách bộ sưu tập
 *     tags: [Collections]
 *     parameters:
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           minimum: 1
 *           default: 1
 *         description: Số trang
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           minimum: 1
 *           maximum: 100
 *           default: 10
 *         description: Số lượng kết quả mỗi trang
 *       - in: query
 *         name: user_id
 *         schema:
 *           type: integer
 *         description: Lọc theo người dùng
 *       - in: query
 *         name: is_private
 *         schema:
 *           type: boolean
 *         description: Lọc theo trạng thái riêng tư
 *     responses:
 *       200:
 *         description: Danh sách bộ sưu tập
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
 *                     $ref: '#/components/schemas/Collection'
 *                 pagination:
 *                   type: object
 *                   properties:
 *                     page:
 *                       type: integer
 *                       example: 1
 *                     limit:
 *                       type: integer
 *                       example: 10
 *                     total:
 *                       type: integer
 *                       example: 25
 */
router.get(
  '/',
  [
    query('page').optional().isInt({ min: 1 }).withMessage('Trang phải là số nguyên dương'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('Giới hạn phải từ 1 đến 100'),
    query('user_id').optional().isInt({ min: 1 }).withMessage('ID người dùng không hợp lệ'),
    query('is_private').optional().isBoolean().withMessage('Giá trị phải là true hoặc false'),
  ],
  validate,
  collectionsController.getCollections
);

/**
 * @swagger
 * /collections/{id}:
 *   get:
 *     summary: Lấy chi tiết bộ sưu tập
 *     tags: [Collections]
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của bộ sưu tập
 *     responses:
 *       200:
 *         description: Thông tin chi tiết bộ sưu tập
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   allOf:
 *                     - $ref: '#/components/schemas/Collection'
 *                     - type: object
 *                       properties:
 *                         items:
 *                           type: array
 *                           items:
 *                             $ref: '#/components/schemas/CollectionItem'
 *                         tags:
 *                           type: array
 *                           items:
 *                             $ref: '#/components/schemas/Tag'
 *                         owner_avatar:
 *                           type: string
 *                         item_count:
 *                           type: integer
 *                         is_liked:
 *                           type: boolean
 *       404:
 *         description: Không tìm thấy bộ sưu tập
 */
router.get(
  '/:id',
  param('id').isInt({ min: 1 }).withMessage('ID bộ sưu tập không hợp lệ'),
  validate,
  collectionsController.getCollectionById
);

/**
 * @swagger
 * /collections/{id}:
 *   put:
 *     summary: Cập nhật thông tin bộ sưu tập
 *     tags: [Collections]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của bộ sưu tập cần cập nhật
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *                 example: "Bộ sưu tập mùa hè 2025 (Đã cập nhật)"
 *               description:
 *                 type: string
 *                 example: "Những sản phẩm tốt nhất mùa hè 2025 - Đã cập nhật"
 *               cover_image_url:
 *                 type: string
 *                 format: uri
 *                 example: "https://example.com/images/updated-summer-collection.jpg"
 *               is_private:
 *                 type: boolean
 *                 example: false
 *               tags:
 *                 type: array
 *                 items:
 *                   type: string
 *                 example: ["mùa hè", "thời trang", "phụ kiện", "cập nhật"]
 *     responses:
 *       200:
 *         description: Cập nhật thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   $ref: '#/components/schemas/Collection'
 *       400:
 *         description: Dữ liệu không hợp lệ
 *       401:
 *         description: Chưa đăng nhập
 *       403:
 *         description: Không có quyền chỉnh sửa bộ sưu tập này
 *       404:
 *         description: Không tìm thấy bộ sưu tập
 */
router.put(
  '/:id',
  authenticateJWT,
  [
    param('id').isInt({ min: 1 }).withMessage('ID bộ sưu tập không hợp lệ'),
    body('name').optional().trim().notEmpty().withMessage('Tên không được để trống'),
    body('description').optional().trim(),
    body('cover_image_url').optional().isURL().withMessage('URL không hợp lệ'),
    body('is_private').optional().isBoolean().withMessage('Giá trị phải là true hoặc false'),
    body('tags').optional().isArray().withMessage('Tags phải là một mảng'),
    body('tags.*').isString().withMessage('Mỗi tag phải là chuỗi'),
  ],
  validate,
  collectionsController.updateCollection
);

/**
 * @swagger
 * /collections/{id}:
 *   delete:
 *     summary: Xóa bộ sưu tập
 *     tags: [Collections]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của bộ sưu tập cần xóa
 *     responses:
 *       200:
 *         description: Xóa bộ sưu tập thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 message:
 *                   type: string
 *                   example: "Đã xóa bộ sưu tập thành công"
 *       401:
 *         description: Chưa đăng nhập
 *       403:
 *         description: Không có quyền xóa bộ sưu tập này
 *       404:
 *         description: Không tìm thấy bộ sưu tập
 */
router.delete(
  '/:id',
  authenticateJWT,
  param('id').isInt({ min: 1 }).withMessage('ID bộ sưu tập không hợp lệ'),
  validate,
  collectionsController.deleteCollection
);

/**
 * @swagger
 * /collections/{id}/items:
 *   post:
 *     summary: Thêm sản phẩm vào bộ sưu tập
 *     tags: [Collections]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của bộ sưu tập
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - item_id
 *             properties:
 *               item_id:
 *                 type: integer
 *                 description: ID của sản phẩm cần thêm
 *                 example: 123
 *               note:
 *                 type: string
 *                 description: Ghi chú cho sản phẩm trong bộ sưu tập
 *                 example: "Sản phẩm rất đẹp, chất lượng tốt"
 *               rating:
 *                 type: integer
 *                 minimum: 1
 *                 maximum: 5
 *                 description: Đánh giá sản phẩm (1-5 sao)
 *                 example: 5
 *     responses:
 *       201:
 *         description: Thêm sản phẩm vào bộ sưu tập thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 message:
 *                   type: string
 *                   example: "Đã thêm sản phẩm vào bộ sưu tập"
 *       400:
 *         description: Dữ liệu không hợp lệ
 *       401:
 *         description: Chưa đăng nhập
 *       403:
 *         description: Không có quyền thêm sản phẩm vào bộ sưu tập này
 *       404:
 *         description: Không tìm thấy bộ sưu tập hoặc sản phẩm
 */
router.post(
  '/:id/items',
  authenticateJWT,
  [
    param('id').isInt({ min: 1 }).withMessage('ID bộ sưu tập không hợp lệ'),
    body('item_id').isInt({ min: 1 }).withMessage('ID sản phẩm không hợp lệ'),
    body('note').optional().trim(),
    body('rating').optional().isInt({ min: 1, max: 5 }).withMessage('Đánh giá phải từ 1 đến 5 sao'),
  ],
  validate,
  collectionsController.addItemToCollection
);

/**
 * @swagger
 * /collections/{collectionId}/items/{itemId}:
 *   delete:
 *     summary: Xóa sản phẩm khỏi bộ sưu tập
 *     tags: [Collections]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: collectionId
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của bộ sưu tập
 *       - in: path
 *         name: itemId
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của sản phẩm cần xóa
 *     responses:
 *       200:
 *         description: Xóa sản phẩm khỏi bộ sưu tập thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 message:
 *                   type: string
 *                   example: "Đã xóa sản phẩm khỏi bộ sưu tập"
 *       401:
 *         description: Chưa đăng nhập
 *       403:
 *         description: Không có quyền xóa sản phẩm khỏi bộ sưu tập này
 *       404:
 *         description: Không tìm thấy bộ sưu tập hoặc sản phẩm
 */
router.delete(
  '/:collectionId/items/:itemId',
  authenticateJWT,
  [
    param('collectionId').isInt({ min: 1 }).withMessage('ID bộ sưu tập không hợp lệ'),
    param('itemId').isInt({ min: 1 }).withMessage('ID sản phẩm không hợp lệ'),
  ],
  validate,
  collectionsController.removeItemFromCollection
);

module.exports = router;
