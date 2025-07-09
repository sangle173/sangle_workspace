const express = require('express');
const router = express.Router();
const reportController = require('../controllers/reportController');

// Report pages
router.get('/', reportController.getReportPage);
router.get('/daily', reportController.getDailyReportPage);
router.get('/weekly', reportController.getWeeklyReportPage);
router.get('/custom', reportController.getCustomReportPage);

// Report generation and sending
router.post('/generate', reportController.generateReport);
router.post('/send', reportController.sendReport);

module.exports = router;
