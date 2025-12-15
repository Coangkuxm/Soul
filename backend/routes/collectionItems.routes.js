const express = require('express');
const router = express.Router();
const { authenticateJWT } = require('../middleware/auth.middleware');
const { validate } = require('../middleware/validation.middleware');
const collectionItemsController = require('../controllers/collectionItems.controller');
const {
  addItemToCollectionValidation,
  getItemsInCollectionValidation,
  removeItemFromCollectionValidation
} = require('../validations/collectionItems.validations');

// Thêm item vào collection
router.post(
  '/:collection_id/items',  // Sửa lại route để phù hợp với URL
  authenticateJWT,
  validate(addItemToCollectionValidation),
  collectionItemsController.addItemToCollection
);

// Xóa item khỏi collection
router.delete(
  '/:collection_id/items/:item_id',
  authenticateJWT,
  validate(removeItemFromCollectionValidation),
  collectionItemsController.removeItemFromCollection
);

// Lấy danh sách items trong collection
router.get(
  '/:collection_id/items',
  validate(getItemsInCollectionValidation),
  collectionItemsController.getItemsInCollection
);

module.exports = router;
