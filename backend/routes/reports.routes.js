const express = require('express');
const router = express.Router();
const reportsController = require('../controllers/reports.controller');
const { authenticateToken, isAdmin } = require('../middlewares/auth.middleware');

/**
 * @swagger
 * /reports:
 *   post:
 *     summary: Gửi báo cáo tài khoản hoặc bài viết
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/ReportCreateRequest'
 *     responses:
 *       201:
 *         description: Gửi báo cáo thành công
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 */
router.post('/', authenticateToken, (req, res, next) =>
  reportsController.createReport(req, res, next)
);

/**
 * @swagger
 * /reports:
 *   get:
 *     summary: Admin xem danh sách báo cáo
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: targetType
 *         schema:
 *           type: string
 *           enum: [user, collection_item]
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [pending, reviewed, dismissed, actioned]
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 20
 *     responses:
 *       200:
 *         description: Danh sách báo cáo
 */
router.get('/', authenticateToken, isAdmin, (req, res, next) =>
  reportsController.listReports(req, res, next)
);

/**
 * @swagger
 * /reports/posts:
 *   get:
 *     summary: Admin xem danh sách bài viết bị báo cáo
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [pending, reviewed, dismissed, actioned]
 *     responses:
 *       200:
 *         description: Danh sách bài viết bị báo cáo
 */
router.get('/posts', authenticateToken, isAdmin, (req, res, next) =>
  reportsController.listReportedPosts(req, res, next)
);

/**
 * @swagger
 * /reports/users:
 *   get:
 *     summary: Admin xem danh sách tài khoản bị báo cáo
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [pending, reviewed, dismissed, actioned]
 *     responses:
 *       200:
 *         description: Danh sách tài khoản bị báo cáo
 */
router.get('/users', authenticateToken, isAdmin, (req, res, next) =>
  reportsController.listReportedUsers(req, res, next)
);

/**
 * @swagger
 * /reports/{id}/status:
 *   patch:
 *     summary: Admin cập nhật trạng thái một báo cáo
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/ReportStatusUpdateRequest'
 *     responses:
 *       200:
 *         description: Cập nhật trạng thái thành công
 */
router.patch('/:id/status', authenticateToken, isAdmin, (req, res, next) =>
  reportsController.updateReportStatus(req, res, next)
);

/**
 * @swagger
 * /reports/posts/{id}/lock:
 *   patch:
 *     summary: Admin khóa bài viết bị báo cáo
 *     tags: [Moderation]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/ModerationReasonRequest'
 *     responses:
 *       200:
 *         description: Khóa bài viết thành công
 */
router.patch('/posts/:id/lock', authenticateToken, isAdmin, (req, res, next) =>
  reportsController.lockPost(req, res, next)
);

/**
 * @swagger
 * /reports/posts/{id}/unlock:
 *   patch:
 *     summary: Admin mở khóa bài viết
 *     tags: [Moderation]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Mở khóa bài viết thành công
 */
router.patch('/posts/:id/unlock', authenticateToken, isAdmin, (req, res, next) =>
  reportsController.unlockPost(req, res, next)
);

/**
 * @swagger
 * /reports/users/{id}/lock:
 *   patch:
 *     summary: Admin khóa tài khoản
 *     tags: [Moderation]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/ModerationReasonRequest'
 *     responses:
 *       200:
 *         description: Khóa tài khoản thành công
 */
router.patch('/users/:id/lock', authenticateToken, isAdmin, (req, res, next) =>
  reportsController.lockUser(req, res, next)
);

/**
 * @swagger
 * /reports/users/{id}/unlock:
 *   patch:
 *     summary: Admin mở khóa tài khoản
 *     tags: [Moderation]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Mở khóa tài khoản thành công
 */
router.patch('/users/:id/unlock', authenticateToken, isAdmin, (req, res, next) =>
  reportsController.unlockUser(req, res, next)
);

module.exports = router;
