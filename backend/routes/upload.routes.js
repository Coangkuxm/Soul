// routes/upload.routes.js
const express = require('express');
const router = express.Router();
const multer = require('multer');
const cloudinary = require('../config/cloudinary');
const { authenticateToken } = require('../middlewares/auth.middleware');

// Cấu hình multer để lưu file tạm trong memory
const storage = multer.memoryStorage();
const upload = multer({
  storage,
  limits: {
    fileSize: 5 * 1024 * 1024, // Giới hạn 5MB
  },
  fileFilter: (req, file, cb) => {
    // Chỉ cho phép upload ảnh
    if (file.mimetype.startsWith('image/')) {
      cb(null, true);
    } else {
      cb(new Error('Chỉ cho phép upload file ảnh!'), false);
    }
  }
});

/**
 * @swagger
 * tags:
 *   name: Upload
 *   description: Upload ảnh lên Cloudinary
 */

/**
 * @swagger
 * /upload/image:
 *   post:
 *     summary: Upload một ảnh
 *     tags: [Upload]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         multipart/form-data:
 *           schema:
 *             type: object
 *             required:
 *               - image
 *             properties:
 *               image:
 *                 type: string
 *                 format: binary
 *                 description: File ảnh cần upload
 *               folder:
 *                 type: string
 *                 description: Thư mục lưu trữ (mặc định là soul-app)
 *                 example: avatars
 *     responses:
 *       200:
 *         description: Upload thành công
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   type: object
 *                   properties:
 *                     url:
 *                       type: string
 *                       example: https://res.cloudinary.com/dwgzlv73n/image/upload/v1234567890/soul-app/abc123.jpg
 *                     public_id:
 *                       type: string
 *                       example: soul-app/abc123
 *                     width:
 *                       type: integer
 *                       example: 800
 *                     height:
 *                       type: integer
 *                       example: 600
 *       400:
 *         description: Không có file hoặc file không hợp lệ
 *       401:
 *         description: Chưa đăng nhập
 *       500:
 *         description: Lỗi server
 */
router.post('/image', authenticateToken, upload.single('image'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        error: 'Vui lòng chọn file ảnh để upload'
      });
    }

    const folder = req.body.folder || 'soul-app';

    // Upload lên Cloudinary
    const result = await new Promise((resolve, reject) => {
      const uploadStream = cloudinary.uploader.upload_stream(
        {
          folder: folder,
          resource_type: 'image',
          transformation: [
            { quality: 'auto' }, // Tự động tối ưu chất lượng
            { fetch_format: 'auto' } // Tự động chọn format tốt nhất
          ]
        },
        (error, result) => {
          if (error) reject(error);
          else resolve(result);
        }
      );
      uploadStream.end(req.file.buffer);
    });

    res.json({
      success: true,
      data: {
        url: result.secure_url,
        public_id: result.public_id,
        width: result.width,
        height: result.height
      }
    });

  } catch (error) {
    console.error('Upload error:', error);
    res.status(500).json({
      success: false,
      error: error.message || 'Có lỗi xảy ra khi upload ảnh'
    });
  }
});

/**
 * @swagger
 * /upload/avatar:
 *   post:
 *     summary: Upload avatar người dùng
 *     tags: [Upload]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         multipart/form-data:
 *           schema:
 *             type: object
 *             required:
 *               - image
 *             properties:
 *               image:
 *                 type: string
 *                 format: binary
 *     responses:
 *       200:
 *         description: Upload avatar thành công
 */
router.post('/avatar', authenticateToken, upload.single('image'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        error: 'Vui lòng chọn file ảnh để upload'
      });
    }

    // Upload với transformation cho avatar
    const result = await new Promise((resolve, reject) => {
      const uploadStream = cloudinary.uploader.upload_stream(
        {
          folder: 'soul-app/avatars',
          resource_type: 'image',
          transformation: [
            { width: 400, height: 400, crop: 'fill', gravity: 'face' }, // Crop vuông, focus vào mặt
            { quality: 'auto' },
            { fetch_format: 'auto' }
          ]
        },
        (error, result) => {
          if (error) reject(error);
          else resolve(result);
        }
      );
      uploadStream.end(req.file.buffer);
    });

    res.json({
      success: true,
      data: {
        url: result.secure_url,
        public_id: result.public_id
      }
    });

  } catch (error) {
    console.error('Avatar upload error:', error);
    res.status(500).json({
      success: false,
      error: error.message || 'Có lỗi xảy ra khi upload avatar'
    });
  }
});

/**
 * @swagger
 * /upload/collection-cover:
 *   post:
 *     summary: Upload ảnh bìa cho collection
 *     tags: [Upload]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         multipart/form-data:
 *           schema:
 *             type: object
 *             required:
 *               - image
 *             properties:
 *               image:
 *                 type: string
 *                 format: binary
 *     responses:
 *       200:
 *         description: Upload thành công
 */
router.post('/collection-cover', authenticateToken, upload.single('image'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        error: 'Vui lòng chọn file ảnh để upload'
      });
    }

    // Upload với transformation cho cover image
    const result = await new Promise((resolve, reject) => {
      const uploadStream = cloudinary.uploader.upload_stream(
        {
          folder: 'soul-app/covers',
          resource_type: 'image',
          transformation: [
            { width: 1200, height: 630, crop: 'fill' }, // Tỉ lệ 1.91:1 (chuẩn social media)
            { quality: 'auto' },
            { fetch_format: 'auto' }
          ]
        },
        (error, result) => {
          if (error) reject(error);
          else resolve(result);
        }
      );
      uploadStream.end(req.file.buffer);
    });

    res.json({
      success: true,
      data: {
        url: result.secure_url,
        public_id: result.public_id,
        width: result.width,
        height: result.height
      }
    });

  } catch (error) {
    console.error('Cover upload error:', error);
    res.status(500).json({
      success: false,
      error: error.message || 'Có lỗi xảy ra khi upload ảnh bìa'
    });
  }
});

/**
 * @swagger
 * /upload/delete:
 *   delete:
 *     summary: Xóa ảnh khỏi Cloudinary
 *     tags: [Upload]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - public_id
 *             properties:
 *               public_id:
 *                 type: string
 *                 example: soul-app/avatars/abc123
 *     responses:
 *       200:
 *         description: Xóa thành công
 */
router.delete('/delete', authenticateToken, async (req, res) => {
  try {
    const { public_id } = req.body;

    if (!public_id) {
      return res.status(400).json({
        success: false,
        error: 'Thiếu public_id của ảnh cần xóa'
      });
    }

    const result = await cloudinary.uploader.destroy(public_id);

    res.json({
      success: true,
      data: result
    });

  } catch (error) {
    console.error('Delete error:', error);
    res.status(500).json({
      success: false,
      error: error.message || 'Có lỗi xảy ra khi xóa ảnh'
    });
  }
});

module.exports = router;
