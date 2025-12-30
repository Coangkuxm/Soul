const Joi = require('joi');

// Schema for adding a Spotify track/album to a collection
const addSpotifyItemToCollectionValidation = {
  body: Joi.object({
    collection_id: Joi.string().uuid().required()
      .messages({
        'string.guid': 'ID collection không hợp lệ',
        'any.required': 'ID collection là bắt buộc'
      }),
    spotify_id: Joi.string().required()
      .messages({
        'string.empty': 'ID Spotify không được để trống',
        'any.required': 'ID Spotify là bắt buộc'
      }),
    spotify_type: Joi.string().valid('track', 'album', 'artist', 'playlist').required()
      .messages({
        'any.only': 'Loại Spotify phải là một trong: track, album, artist, playlist',
        'any.required': 'Loại Spotify là bắt buộc'
      }),
    metadata: Joi.object().optional()
      .messages({
        'object.base': 'Metadata phải là một đối tượng'
      })
  })
};

// Schema for getting Spotify items in a collection
const getSpotifyItemsInCollectionValidation = {
  params: Joi.object({
    collection_id: Joi.string().uuid().required()
      .messages({
        'string.guid': 'ID collection không hợp lệ',
        'any.required': 'ID collection là bắt buộc'
      })
  }),
  query: Joi.object({
    page: Joi.number().integer().min(1).default(1)
      .messages({
        'number.base': 'Số trang phải là một số',
        'number.integer': 'Số trang phải là số nguyên',
        'number.min': 'Số trang tối thiểu là 1'
      }),
    limit: Joi.number().integer().min(1).max(100).default(10)
      .messages({
        'number.base': 'Giới hạn phải là một số',
        'number.integer': 'Giới hạn phải là số nguyên',
        'number.min': 'Giới hạn tối thiểu là 1',
        'number.max': 'Giới hạn tối đa là 100'
      })
  })
};

// Schema for removing a Spotify item from a collection
const removeSpotifyItemFromCollectionValidation = {
  params: Joi.object({
    collection_id: Joi.string().uuid().required()
      .messages({
        'string.guid': 'ID collection không hợp lệ',
        'any.required': 'ID collection là bắt buộc'
      }),
    spotify_id: Joi.string().required()
      .messages({
        'string.empty': 'ID Spotify không được để trống',
        'any.required': 'ID Spotify là bắt buộc'
      })
  })
};

module.exports = {
  addSpotifyItemToCollectionValidation,
  getSpotifyItemsInCollectionValidation,
  removeSpotifyItemFromCollectionValidation
};
