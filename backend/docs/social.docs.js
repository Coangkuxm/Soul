/**
 * @swagger
 * tags:
 *   name: Social
 *   description: Tương tác xã hội (thích, bình luận, theo dõi)
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     LikeRequest:
 *       type: object
 *       required:
 *         - targetId
 *         - targetType
 *       properties:
 *         targetId:
 *           type: integer
 *           description: ID của đối tượng muốn thích (collection/item/comment)
 *           example: 1
 *         targetType:
 *           type: string
 *           enum: [collection, item, comment]
 *           description: Loại đối tượng muốn thích
 *           example: "collection"
 * 
 *     CommentRequest:
 *       type: object
 *       required:
 *         - content
 *         - targetId
 *         - targetType
 *       properties:
 *         content:
 *           type: string
 *           description: Nội dung bình luận
 *           example: "Bộ sưu tập rất đẹp!"
 *         targetId:
 *           type: integer
 *           description: ID của đối tượng muốn bình luận (collection/item)
 *           example: 1
 *         targetType:
 *           type: string
 *           enum: [collection, item]
 *           description: Loại đối tượng muốn bình luận
 *           example: "collection"
 * 
 *     CommentResponse:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *           example: 1
 *         user_id:
 *           type: integer
 *           example: 1
 *         username:
 *           type: string
 *           example: "nguoidung1"
 *         avatar_url:
 *           type: string
 *           example: "https://example.com/avatar.jpg"
 *         content:
 *           type: string
 *           example: "Bình luận mẫu"
 *         created_at:
 *           type: string
 *           format: date-time
 *           example: "2023-01-01T00:00:00.000Z"
 * 
 *     NotificationResponse:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *           example: 1
 *         notification_type:
 *           type: string
 *           enum: [like, comment, follow, mention]
 *           example: "like"
 *         is_read:
 *           type: boolean
 *           example: false
 *         created_at:
 *           type: string
 *           format: date-time
 *           example: "2023-01-01T00:00:00.000Z"
 *         sender_username:
 *           type: string
 *           example: "nguoidung2"
 *         sender_avatar:
 *           type: string
 *           example: "https://example.com/avatar2.jpg"
 *         target_id:
 *           type: integer
 *           example: 1
 *         target_type:
 *           type: string
 *           example: "collection"
 */

/**
 * @swagger
 * /social/users/{id}/follow:
 *   post:
 *     summary: Theo dõi người dùng
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của người dùng muốn theo dõi
 *     responses:
 *       200:
 *         description: Đã theo dõi người dùng thành công
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 *   delete:
 *     summary: Hủy theo dõi người dùng
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của người dùng muốn hủy theo dõi
 *     responses:
 *       200:
 *         description: Đã hủy theo dõi thành công
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 * /social/likes:
 *   post:
 *     summary: Thích một đối tượng (collection/item/comment)
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/LikeRequest'
 *     responses:
 *       201:
 *         description: Đã thích thành công
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 *   delete:
 *     summary: Bỏ thích một đối tượng
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/LikeRequest'
 *     responses:
 *       200:
 *         description: Đã bỏ thích thành công
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 * /social/comments:
 *   post:
 *     summary: Tạo bình luận mới
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CommentRequest'
 *     responses:
 *       201:
 *         description: Đã tạo bình luận thành công
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/CommentResponse'
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 * /social/comments/{targetType}/{targetId}:
 *   get:
 *     summary: Lấy danh sách bình luận
 *     tags: [Social]
 *     parameters:
 *       - in: path
 *         name: targetType
 *         required: true
 *         schema:
 *           type: string
 *           enum: [collection, item]
 *         description: Loại đối tượng
 *       - in: path
 *         name: targetId
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của đối tượng
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *         description: Số trang
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 10
 *         description: Số lượng bản ghi mỗi trang
 *     responses:
 *       200:
 *         description: Danh sách bình luận
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
 *                     $ref: '#/components/schemas/CommentResponse'
 *                 pagination:
 *                   type: object
 *                   properties:
 *                     total:
 *                       type: integer
 *                       example: 1
 *                     page:
 *                       type: integer
 *                       example: 1
 *                     limit:
 *                       type: integer
 *                       example: 10
 *                     totalPages:
 *                       type: integer
 *                       example: 1
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 * /social/comments/{id}:
 *   put:
 *     summary: Cập nhật bình luận
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của bình luận
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - content
 *             properties:
 *               content:
 *                 type: string
 *                 description: Nội dung mới của bình luận
 *     responses:
 *       200:
 *         description: Đã cập nhật bình luận thành công
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/CommentResponse'
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       403:
 *         description: Không có quyền chỉnh sửa bình luận này
 *       404:
 *         description: Không tìm thấy bình luận
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 *   delete:
 *     summary: Xóa bình luận
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của bình luận
 *     responses:
 *       200:
 *         description: Đã xóa bình luận thành công
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       403:
 *         description: Không có quyền xóa bình luận này
 *       404:
 *         description: Không tìm thấy bình luận
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 * /social/notifications:
 *   get:
 *     summary: Lấy danh sách thông báo
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *         description: Số trang
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 20
 *         description: Số lượng thông báo mỗi trang
 *       - in: query
 *         name: unreadOnly
 *         schema:
 *           type: boolean
 *           default: false
 *         description: Chỉ lấy thông báo chưa đọc
 *     responses:
 *       200:
 *         description: Danh sách thông báo
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
 *                     $ref: '#/components/schemas/NotificationResponse'
 *                 pagination:
 *                   type: object
 *                   properties:
 *                     total:
 *                       type: integer
 *                       example: 1
 *                     page:
 *                       type: integer
 *                       example: 1
 *                     limit:
 *                       type: integer
 *                       example: 20
 *                     totalPages:
 *                       type: integer
 *                       example: 1
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       500:
 *         $ref: '#/components/responses/ServerError'
 *
 * /social/notifications/{id}/read:
 *   put:
 *     summary: Đánh dấu thông báo là đã đọc
 *     tags: [Social]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID của thông báo
 *     responses:
 *       200:
 *         description: Đã đánh dấu đã đọc thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   $ref: '#/components/schemas/NotificationResponse'
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       404:
 *         description: Không tìm thấy thông báo
 *       500:
 *         $ref: '#/components/responses/ServerError'*/
