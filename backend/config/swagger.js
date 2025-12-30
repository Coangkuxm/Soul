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
              format: 'email',
              example: 'user@gmail.com'
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
      responses: {
        BadRequest: {
          description: 'Bad Request - The request was invalid or cannot be served',
          content: {
            'application/json': {
              schema: {
                type: 'object',
                properties: {
                  success: { type: 'boolean', example: false },
                  error: {
                    type: 'object',
                    properties: {
                      code: { type: 'string', example: 'BAD_REQUEST' },
                      message: { type: 'string', example: 'Invalid input data' },
                      details: { type: 'array', items: { type: 'object' } }
                    }
                  }
                }
              }
            }
          }
        },
        Unauthorized: {
          description: 'Unauthorized - Authentication failed or user does not have permissions',
          content: {
            'application/json': {
              schema: {
                type: 'object',
                properties: {
                  success: { type: 'boolean', example: false },
                  error: {
                    type: 'object',
                    properties: {
                      code: { type: 'string', example: 'UNAUTHORIZED' },
                      message: { type: 'string', example: 'Authentication required' },
                      details: { type: 'array', items: { type: 'object' } }
                    }
                  }
                }
              }
            }
          }
        },
        ServerError: {
          description: 'Server Error - Something went wrong on the server',
          content: {
            'application/json': {
              schema: {
                type: 'object',
                properties: {
                  success: { type: 'boolean', example: false },
                  error: {
                    type: 'object',
                    properties: {
                      code: { type: 'string', example: 'INTERNAL_SERVER_ERROR' },
                      message: { type: 'string', example: 'An unexpected error occurred' },
                      details: { type: 'array', items: { type: 'object' } }
                    }
                  }
                }
              }
            }
          }
        }
      },
    },
    paths: {
      '/account/forgot-password': {
        post: {
          tags: ['Account'],
          summary: 'Gửi yêu cầu đặt lại mật khẩu',
          description: 'Gửi email chứa liên kết đặt lại mật khẩu đến email đăng ký',
          requestBody: {
            required: true,
            content: {
              'application/json': {
                schema: {
                  type: 'object',
                  required: ['email'],
                  properties: {
                    email: {
                      type: 'string',
                      format: 'email',
                      example: 'user@gmail.com'
                    }
                  }
                }
              }
            }
          },
          responses: {
            200: {
              description: 'Yêu cầu thành công, kiểm tra email của bạn',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      success: { type: 'boolean', example: true },
                      message: { type: 'string' }
                    }
                  }
                }
              }
            },
            400: { $ref: '#/components/responses/BadRequest' },
            500: { $ref: '#/components/responses/ServerError' }
          }
        }
      },
      '/account/reset-password': {
        post: {
          tags: ['Account'],
          summary: 'Đặt lại mật khẩu',
          description: 'Đặt lại mật khẩu bằng token từ email',
          requestBody: {
            required: true,
            content: {
              'application/json': {
                schema: {
                  type: 'object',
                  required: ['token', 'password'],
                  properties: {
                    token: {
                      type: 'string',
                      description: 'Token đặt lại mật khẩu từ email',
                      example: 'a1b2c3d4e5f6g7h8i9j0'
                    },
                    password: {
                      type: 'string',
                      format: 'password',
                      minLength: 6,
                      example: 'newSecurePassword123'
                    }
                  }
                }
              }
            }
          },
          responses: {
            200: {
              description: 'Đặt lại mật khẩu thành công',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      success: { type: 'boolean', example: true },
                      message: { type: 'string' }
                    }
                  }
                }
              }
            },
            400: { $ref: '#/components/responses/BadRequest' },
            401: { $ref: '#/components/responses/Unauthorized' },
            500: { $ref: '#/components/responses/ServerError' }
          }
        }
      },
      '/account/send-verification-email': {
        get: {
          tags: ['Account'],
          summary: 'Gửi lại email xác thực',
          description: 'Gửi lại email xác thực cho người dùng đã đăng nhập',
          security: [{ bearerAuth: [] }],
          responses: {
            200: {
              description: 'Email xác thực đã được gửi',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      success: { type: 'boolean', example: true },
                      message: { type: 'string' }
                    }
                  }
                }
              }
            },
            400: { $ref: '#/components/responses/BadRequest' },
            401: { $ref: '#/components/responses/Unauthorized' },
            500: { $ref: '#/components/responses/ServerError' }
          }
        }
      },
      '/account/verify-email': {
        post: {
          tags: ['Account'],
          summary: 'Xác thực email',
          description: 'Xác thực địa chỉ email bằng token',
          requestBody: {
            required: true,
            content: {
              'application/json': {
                schema: {
                  type: 'object',
                  required: ['token'],
                  properties: {
                    token: {
                      type: 'string',
                      description: 'Token xác thực từ email',
                      example: 'a1b2c3d4e5f6g7h8i9j0'
                    }
                  }
                }
              }
            }
          },
          responses: {
            200: {
              description: 'Email đã được xác thực thành công',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      success: { type: 'boolean', example: true },
                      message: { type: 'string' }
                    }
                  }
                }
              }
            },
            400: { $ref: '#/components/responses/BadRequest' },
            401: { $ref: '#/components/responses/Unauthorized' },
            500: { $ref: '#/components/responses/ServerError' }
          }
        }
      },
      '/account/check-email-verification': {
        get: {
          tags: ['Account'],
          summary: 'Kiểm tra trạng thái xác thực email',
          description: 'Kiểm tra xem email của người dùng đã được xác thực chưa',
          security: [{ bearerAuth: [] }],
          responses: {
            200: {
              description: 'Trạng thái xác thực email',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      success: { type: 'boolean', example: true },
                      data: {
                        type: 'object',
                        properties: {
                          email_verified: { type: 'boolean' }
                        }
                      }
                    }
                  }
                }
              }
            },
            401: { $ref: '#/components/responses/Unauthorized' },
            500: { $ref: '#/components/responses/ServerError' }
          }
        }
      }
    }
  },
  apis: [
    './routes/*.js',
    './docs/*.js'  // Include all .js files in the docs directory
  ],
};

const specs = swaggerJsdoc(options);
module.exports = specs;