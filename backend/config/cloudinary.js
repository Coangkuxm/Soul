// config/cloudinary.js
const cloudinary = require('cloudinary').v2;

// Cloudinary SDK tự đọc biến CLOUDINARY_URL nếu có -> cảnh báo vì nó có thể ghi đè cloud_name
if (process.env.CLOUDINARY_URL) {
  console.warn(
    '[Cloudinary] Phát hiện biến CLOUDINARY_URL trên môi trường này. ' +
    'Nó sẽ GHI ĐÈ cloud_name/api_key. Nếu sai (vd ...@Root) hãy XÓA biến này trên Render.'
  );
}

// .trim() để loại khoảng trắng/ký tự thừa khi dán giá trị trên dashboard
const cloudName = (process.env.CLOUDINARY_CLOUD_NAME || '').trim();
const apiKey = (process.env.CLOUDINARY_API_KEY || '').trim();
const apiSecret = (process.env.CLOUDINARY_API_SECRET || '').trim();

cloudinary.config({
  cloud_name: cloudName,
  api_key: apiKey,
  api_secret: apiSecret,
  secure: true
});

// Log GIÁ TRỊ THỰC SỰ mà SDK đang dùng (không chỉ env var) để chẩn đoán trên Render
const effective = cloudinary.config();
const missing = [];
if (!effective.cloud_name) missing.push('CLOUDINARY_CLOUD_NAME');
if (!effective.api_key) missing.push('CLOUDINARY_API_KEY');
if (!effective.api_secret) missing.push('CLOUDINARY_API_SECRET');

if (missing.length > 0) {
  console.error(
    `[Cloudinary] Thiếu env vars: ${missing.join(', ')}. Mọi upload ảnh sẽ thất bại (500).`
  );
} else {
  console.log(`[Cloudinary] Đang dùng cloud_name="${effective.cloud_name}" (api_key đuôi ...${effective.api_key.slice(-4)})`);
}

module.exports = cloudinary;
