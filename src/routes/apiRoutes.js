const express = require('express');
const router = express.Router();
const apiController = require('../controllers/apiController');

// API status check
router.get('/status', apiController.checkApiStatus);

module.exports = router;
