const { body, param } = require('express-validator');

const addItemToCollectionValidation = [
  body('collection_id')
    .isInt({ min: 1 })
    .withMessage('ID collection không hợp lệ')
    .toInt(),
  body('item_id')
    .isInt({ min: 1 })
    .withMessage('ID item không hợp lệ')
    .toInt()
];

const getItemsInCollectionValidation = [
  param('collection_id')
    .isInt({ min: 1 })
    .withMessage('ID collection không hợp lệ')
    .toInt(),
  param('page')
    .optional()
    .isInt({ min: 1 })
    .withMessage('Số trang phải là số nguyên dương')
    .toInt(),
  param('limit')
    .optional()
    .isInt({ min: 1, max: 100 })
    .withMessage('Giới hạn phải từ 1 đến 100')
    .toInt()
];

const removeItemFromCollectionValidation = [
  param('collection_id')
    .isInt({ min: 1 })
    .withMessage('ID collection không hợp lệ')
    .toInt(),
  param('item_id')
    .isInt({ min: 1 })
    .withMessage('ID item không hợp lệ')
    .toInt()
];

module.exports = {
  addItemToCollectionValidation,
  getItemsInCollectionValidation,
  removeItemFromCollectionValidation
};
