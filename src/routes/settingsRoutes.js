const express = require('express');
const router = express.Router();
const settingsController = require('../controllers/settingsController');

// Settings routes
router.get('/', settingsController.getSettingsPage);
router.post('/update-api', settingsController.updateApiEndpoint);

module.exports = router;
