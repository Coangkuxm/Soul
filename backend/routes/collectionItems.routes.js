const express = require('express');
const router = express.Router();
const { authenticateToken } = require('../middlewares/auth.middleware');
const { validate } = require('../middlewares/validation.middleware');
const collectionItemsController = require('../controllers/collectionItems.controller');
const {
  addItemToCollectionValidation,
  getItemsInCollectionValidation,
  removeItemFromCollectionValidation
} = require('../validations/collectionItems.validations');

const {
  addSpotifyItemToCollectionValidation,
  getSpotifyItemsInCollectionValidation,
  removeSpotifyItemFromCollectionValidation
} = require('../validations/spotify.validations');

// ==================================
// Regular Item Routes
// ==================================

// Thêm item thường vào collection
router.post(
  '/:collection_id/items',
  authenticateToken,
  validate(addItemToCollectionValidation),
  collectionItemsController.addItemToCollection
);

// Xóa item thường khỏi collection
router.delete(
  '/:collection_id/items/:item_id',
  authenticateToken,
  validate(removeItemFromCollectionValidation),
  collectionItemsController.removeItemFromCollection
);

// ==================================
// Spotify Item Routes
// ==================================

// Thêm Spotify item vào collection bằng ID trực tiếp
router.post(
  '/:collection_id/spotify-items',
  authenticateToken,
  validate(addSpotifyItemToCollectionValidation),
  collectionItemsController.addSpotifyItemToCollection
);

// Thêm Spotify item vào collection bằng URL
router.post(
  '/:collection_id/spotify/url',
  authenticateToken,
  collectionItemsController.addSpotifyItemByUrl
);

// Xóa Spotify item khỏi collection
router.delete(
  '/:collection_id/spotify-items/:spotify_id',
  authenticateToken,
  validate(removeSpotifyItemFromCollectionValidation),
  collectionItemsController.removeSpotifyItemFromCollection
);

// ==================================
// General Collection Routes
// ==================================

// Lấy danh sách items trong collection (cả thường và Spotify)
router.get(
  '/:collection_id/items',
  validate(getItemsInCollectionValidation),
  collectionItemsController.getItemsInCollection
);

module.exports = router;
