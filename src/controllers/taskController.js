const taskModel = require('../models/taskModel');
const { startOfWeek, endOfWeek } = require('date-fns');

exports.getHomePage = async (req, res) => {
  try {
    // Set API base URL from session if available
    if (req.session.apiBaseUrl) {
      taskModel.setApiBaseUrl(req.session.apiBaseUrl);
    }
    
    // Get selected board ID from query params or use default
    const boardId = req.query.boardId || process.env.DEFAULT_BOARD_ID || '1';
    
    // Fetch boards
    const boardsResponse = await taskModel.getAllBoards();
    const boards = boardsResponse.boards || [];
    
    // Find current board name
    const currentBoard = boards.find(board => board.id.toString() === boardId.toString()) || { id: boardId, name: 'Unknown Board' };
    
    // Fetch tasks for the selected board
    const response = await taskModel.getAllTasks(boardId);
    
    res.render('index', { 
      title: 'Task Report Generator',
      tasks: response.data.tasks,
      count: response.data.count,
      boards: boards,
      currentBoardId: boardId,
      currentBoard: currentBoard,
      apiEndpoint: req.session.apiBaseUrl || process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api',
      error: null
    });
  } catch (error) {
    // Try to get boards even if tasks fail
    let boards = [];
    try {
      const boardsResponse = await taskModel.getAllBoards();
      boards = boardsResponse.boards || [];
    } catch (boardError) {
      console.error('Failed to fetch boards:', boardError);
    }
    
    res.render('index', { 
      title: 'Task Report Generator',
      tasks: [],
      count: 0,
      boards: boards,
      currentBoardId: req.query.boardId || process.env.DEFAULT_BOARD_ID || '1',
      currentBoard: { name: 'Unknown Board' },
      apiEndpoint: req.session.apiBaseUrl || process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api',
      error: error.message 
    });
  }
};

exports.getAllTasks = async (req, res) => {
  try {
    const boardId = req.query.boardId || process.env.DEFAULT_BOARD_ID;
    const response = await taskModel.getAllTasks(boardId);
    res.json(response);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getTasksByDate = async (req, res) => {
  try {
    const { date, boardId } = req.query;
    if (!date) {
      return res.status(400).json({ error: 'Date parameter is required' });
    }
    
    const tasks = await taskModel.getTasksByDate(date, boardId);
    res.json({ date, tasks, count: tasks.length });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getTasksByDateRange = async (req, res) => {
  try {
    const { startDate, endDate, boardId } = req.query;
    if (!startDate || !endDate) {
      return res.status(400).json({ error: 'Start date and end date parameters are required' });
    }
    
    const tasks = await taskModel.getTasksByDateRange(startDate, endDate, boardId);
    res.json({ startDate, endDate, tasks, count: tasks.length });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getAllBoards = async (req, res) => {
  try {
    const response = await taskModel.getAllBoards();
    res.json(response);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};
