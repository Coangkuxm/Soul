const express = require('express');
const router = express.Router();
const { body, param, query } = require('express-validator');
const itemsController = require('../controllers/items.controller');
const { authenticateJWT } = require('../middleware/auth.middleware');
const { validate } = require('../middleware/validation.middleware');

/**
 * @swagger
 * tags:
 *   name: Items
 *   description: Quản lý items trong hệ thống
 */

// Get all items with pagination and filtering
/**
 * @swagger
 * /items:
 *   get:
 *     summary: Lấy danh sách items
 *     tags: [Items]
 *     parameters:
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *         description: Số trang hiện tại
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 10
 *         description: Số lượng items mỗi trang
 *       - in: query
 *         name: search
 *         schema:
 *           type: string
 *         description: Từ khóa tìm kiếm
 *     responses:
 *       200:
 *         description: Danh sách items
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: array
 *                   items:
 *                     $ref: '#/components/schemas/Item'
 *                 pagination:
 *                   $ref: '#/components/schemas/Pagination'
 */
router.get(
  '/',
  validate([
    query('page').optional().isInt({ min: 1 }).toInt(),
    query('limit').optional().isInt({ min: 1, max: 100 }).toInt(),
    query('search').optional().trim()
  ]),
  itemsController.getAllItems
);

// Get item by ID
/**
 * @swagger
 * /items/{id}:
 *   get:
 *     summary: Lấy thông tin chi tiết item
 *     tags: [Items]
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của item
 *     responses:
 *       200:
 *         description: Thông tin chi tiết item
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   $ref: '#/components/schemas/Item'
 *       404:
 *         description: Không tìm thấy item
 */
router.get(
  '/:id',
  validate([
    param('id').isInt({ min: 1 }).withMessage('ID không hợp lệ')
  ]),
  itemsController.getItemById
);

// Create new item (requires authentication)
/**
 * @swagger
 * /items:
 *   post:
 *     summary: Tạo mới item
 *     tags: [Items]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/ItemInput'
 *     responses:
 *       201:
 *         description: Item đã được tạo thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 message:
 *                   type: string
 *                 data:
 *                   $ref: '#/components/schemas/Item'
 *       400:
 *         description: Dữ liệu không hợp lệ
 *       401:
 *         description: Chưa đăng nhập
 */
router.post(
  '/',
  authenticateJWT,
  itemsController.validateItem,
  validate(),
  itemsController.createItem
);

// Update item (requires authentication and ownership)
/**
 * @swagger
 * /items/{id}:
 *   put:
 *     summary: Cập nhật thông tin item
 *     tags: [Items]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của item cần cập nhật
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/ItemInput'
 *     responses:
 *       200:
 *         description: Item đã được cập nhật thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 message:
 *                   type: string
 *                 data:
 *                   $ref: '#/components/schemas/Item'
 *       400:
 *         description: Dữ liệu không hợp lệ
 *       401:
 *         description: Chưa đăng nhập
 *       403:
 *         description: Không có quyền cập nhật item này
 *       404:
 *         description: Không tìm thấy item
 */
router.put(
  '/:id',
  authenticateJWT,
  validate([
    param('id').isInt({ min: 1 }).withMessage('ID không hợp lệ')
  ]),
  itemsController.validateItem,
  validate(),
  itemsController.updateItem
);

// Delete item (requires authentication and ownership)
/**
 * @swagger
 * /items/{id}:
 *   delete:
 *     summary: Xóa item
 *     tags: [Items]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của item cần xóa
 *     responses:
 *       200:
 *         description: Item đã được xóa thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 message:
 *                   type: string
 *       401:
 *         description: Chưa đăng nhập
 *       403:
 *         description: Không có quyền xóa item này
 *       404:
 *         description: Không tìm thấy item
 */
router.delete(
  '/:id',
  authenticateJWT,
  validate([
    param('id').isInt({ min: 1 }).withMessage('ID không hợp lệ')
  ]),
  itemsController.deleteItem
);

// Search items
/**
 * @swagger
 * /items/search:
 *   get:
 *     summary: Tìm kiếm items
 *     tags: [Items]
 *     parameters:
 *       - in: query
 *         name: q
 *         required: true
 *         schema:
 *           type: string
 *         description: Từ khóa tìm kiếm
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *         description: Số trang hiện tại
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 10
 *         description: Số lượng items mỗi trang
 *     responses:
 *       200:
 *         description: Kết quả tìm kiếm
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: array
 *                   items:
 *                     $ref: '#/components/schemas/Item'
 *                 pagination:
 *                   $ref: '#/components/schemas/Pagination'
 *       400:
 *         description: Thiếu từ khóa tìm kiếm
 */
router.get(
  '/search',
  validate([
    query('q').notEmpty().withMessage('Vui lòng nhập từ khóa tìm kiếm'),
    query('page').optional().isInt({ min: 1 }).toInt(),
    query('limit').optional().isInt({ min: 1, max: 100 }).toInt()
  ]),
  itemsController.searchItems
);

module.exports = router;
