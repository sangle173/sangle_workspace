const express = require('express');
const router = express.Router();
const taskController = require('../controllers/taskController');

// Home page route
router.get('/', taskController.getHomePage);

// API routes
router.get('/api/tasks', taskController.getAllTasks);
router.get('/api/tasks/by-date', taskController.getTasksByDate);
router.get('/api/tasks/by-range', taskController.getTasksByDateRange);
router.get('/api/boards', taskController.getAllBoards);

module.exports = router;
