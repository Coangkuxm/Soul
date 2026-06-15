// config/cloudinary.js
const cloudinary = require('cloudinary').v2;

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET
});

// Cảnh báo ngay lúc boot nếu thiếu config, để upload không "fail im lặng" trên Render
const missingCloudinaryVars = [
  'CLOUDINARY_CLOUD_NAME',
  'CLOUDINARY_API_KEY',
  'CLOUDINARY_API_SECRET'
].filter((name) => !process.env[name]);

if (missingCloudinaryVars.length > 0) {
  console.error(
    `[Cloudinary] Thiếu env vars: ${missingCloudinaryVars.join(', ')}. ` +
    'Mọi upload ảnh sẽ thất bại (500). Hãy set các biến này trong Environment của service.'
  );
} else {
  console.log('[Cloudinary] Cấu hình OK (cloud_name=' + process.env.CLOUDINARY_CLOUD_NAME + ')');
}

module.exports = cloudinary;
