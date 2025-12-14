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