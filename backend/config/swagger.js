// config/swagger.js
const swaggerJsdoc = require('swagger-jsdoc');

const options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Soul API',
      version: '1.0.0',
      description: 'Tài liệu API cho ứng dụng Soul',
    },
    servers: [
      {
        url: 'http://localhost:5000/api',
        description: 'Development server',
      },
      {
        url: 'https://soul-rqnu.onrender.com/api',
        description: 'Production server',
      }
    ],
    components: {
      schemas: {
        User: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              format: 'int64'
            },
            username: {
              type: 'string'
            },
            email: {
              type: 'string',
              format: 'email'
            },
            displayName: {
              type: 'string'
            },
            avatarUrl: {
              type: 'string',
              format: 'uri'
            },
            bio: {
              type: 'string'
            },
            createdAt: {
              type: 'string',
              format: 'date-time'
            },
            updatedAt: {
              type: 'string',
              format: 'date-time'
            }
          }
        },
        Collection: {
          type: 'object',
          properties: {
            id: { type: 'integer', format: 'int64' },
            name: { type: 'string' },
            description: { type: 'string' },
            cover_image_url: { type: 'string', format: 'uri' },
            is_private: { type: 'boolean', default: false },
            owner_id: { type: 'integer', format: 'int64' },
            view_count: { type: 'integer', default: 0 },
            like_count: { type: 'integer', default: 0 },
            comment_count: { type: 'integer', default: 0 },
            created_at: { type: 'string', format: 'date-time' },
            updated_at: { type: 'string', format: 'date-time' }
          }
        },
        CollectionItem: {
          type: 'object',
          properties: {
            id: { type: 'integer', format: 'int64' },
            collection_id: { type: 'integer', format: 'int64' },
            item_id: { type: 'integer', format: 'int64' },
            note: { type: 'string' },
            rating: { type: 'integer', minimum: 1, maximum: 5 },
            added_at: { type: 'string', format: 'date-time' }
          }
        },
        Tag: {
          type: 'object',
          properties: {
            id: { type: 'integer', format: 'int64' },
            name: { type: 'string' }
          }
        },
        Item: {
          type: 'object',
          properties: {
            id: { type: 'integer' },
            type: { 
              type: 'string',
              enum: ['book', 'movie', 'music', 'artist', 'game', 'other']
            },
            title: { type: 'string' },
            description: { type: 'string' },
            cover_image_url: { type: 'string', format: 'url' },
            external_id: { type: 'string' },
            metadata: { type: 'object' },
            created_by: { type: 'integer' },
            created_at: { type: 'string', format: 'date-time' },
            updated_at: { type: 'string', format: 'date-time' },
            creator_username: { type: 'string' },
            creator_avatar: { type: 'string', format: 'url' }
          }
        },
        ItemInput: {
          type: 'object',
          required: ['type', 'title'],
          properties: {
            type: { 
              type: 'string',
              enum: ['book', 'movie', 'music', 'artist', 'game', 'other'],
              description: 'Loại item'
            },
            title: { 
              type: 'string',
              maxLength: 255,
              description: 'Tiêu đề item (1-255 ký tự)'
            },
            description: { 
              type: 'string',
              description: 'Mô tả chi tiết'
            },
            cover_image_url: { 
              type: 'string',
              format: 'url',
              description: 'URL ảnh bìa'
            },
            external_id: { 
              type: 'string',
              description: 'ID từ dịch vụ bên thứ 3'
            },
            metadata: { 
              type: 'object',
              description: 'Dữ liệu bổ sung dạng JSON'
            }
          }
        },
        Pagination: {
          type: 'object',
          properties: {
            page: { type: 'integer' },
            limit: { type: 'integer' },
            total: { type: 'integer' },
            total_pages: { type: 'integer' }
          }
        }
      },
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
        },
      },
    },
  },
  apis: ['./routes/*.js'],
};

const specs = swaggerJsdoc(options);
module.exports = specs;