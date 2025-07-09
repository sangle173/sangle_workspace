const taskModel = require('../models/taskModel');
const { format, startOfWeek, endOfWeek, addDays } = require('date-fns');

exports.getReportPage = async (req, res) => {
  try {
    const reportType = req.query.type || 'daily';
    const boardId = req.query.boardId || process.env.DEFAULT_BOARD_ID || '1';
    
    // Fetch boards
    const boardsResponse = await taskModel.getAllBoards();
    const boards = boardsResponse.boards || [];
    
    res.render('reports', {
      title: 'Generate Report',
      reportType,
      boards,
      currentBoardId: boardId
    });
  } catch (error) {
    res.render('error', {
      title: 'Error',
      message: 'Failed to load report page',
      error: error.message
    });
  }
};

exports.getDailyReportPage = async (req, res) => {
  try {
    const boardId = req.query.boardId || process.env.DEFAULT_BOARD_ID || '1';
    
    // Fetch boards
    const boardsResponse = await taskModel.getAllBoards();
    const boards = boardsResponse.boards || [];
    
    // Find current board name
    const currentBoard = boards.find(board => board.id.toString() === boardId.toString()) || { id: boardId, name: 'Unknown Board' };
    
    res.render('reports/daily', {
      title: 'Daily Report',
      date: new Date().toISOString().split('T')[0],
      boards,
      currentBoardId: boardId,
      currentBoard,
      emailConfig: {
        subject: `Daily Task Report - ${format(new Date(), 'yyyy-MM-dd')} - ${currentBoard.name}`,
        to: '',
        cc: ''
      }
    });
  } catch (error) {
    res.render('error', {
      title: 'Error',
      message: 'Failed to load daily report page',
      error: error.message
    });
  }
};

exports.getWeeklyReportPage = async (req, res) => {
  try {
    const boardId = req.query.boardId || process.env.DEFAULT_BOARD_ID || '1';
    const today = new Date();
    const weekStart = startOfWeek(today);
    const weekEnd = endOfWeek(today);
    
    // Fetch boards
    const boardsResponse = await taskModel.getAllBoards();
    const boards = boardsResponse.boards || [];
    
    // Find current board name
    const currentBoard = boards.find(board => board.id.toString() === boardId.toString()) || { id: boardId, name: 'Unknown Board' };
    
    res.render('reports/weekly', {
      title: 'Weekly Report',
      weekStart: format(weekStart, 'yyyy-MM-dd'),
      weekEnd: format(weekEnd, 'yyyy-MM-dd'),
      boards,
      currentBoardId: boardId,
      currentBoard,
      emailConfig: {
        subject: `Weekly Task Report (${format(weekStart, 'yyyy-MM-dd')} - ${format(weekEnd, 'yyyy-MM-dd')}) - ${currentBoard.name}`,
        to: '',
        cc: ''
      }
    });
  } catch (error) {
    res.render('error', {
      title: 'Error',
      message: 'Failed to load weekly report page',
      error: error.message
    });
  }
};

exports.getCustomReportPage = async (req, res) => {
  try {
    const boardId = req.query.boardId || process.env.DEFAULT_BOARD_ID || '1';
    const today = new Date();
    const defaultEnd = addDays(today, 6);
    
    // Fetch boards
    const boardsResponse = await taskModel.getAllBoards();
    const boards = boardsResponse.boards || [];
    
    // Find current board name
    const currentBoard = boards.find(board => board.id.toString() === boardId.toString()) || { id: boardId, name: 'Unknown Board' };
    
    res.render('reports/custom', {
      title: 'Custom Date Range Report',
      startDate: format(today, 'yyyy-MM-dd'),
      endDate: format(defaultEnd, 'yyyy-MM-dd'),
      boards,
      currentBoardId: boardId,
      currentBoard,
      emailConfig: {
        subject: `Custom Date Range Task Report - ${currentBoard.name}`,
        to: '',
        cc: ''
      }
    });
  } catch (error) {
    res.render('error', {
      title: 'Error',
      message: 'Failed to load custom report page',
      error: error.message
    });
  }
};

exports.generateReport = async (req, res) => {
  try {
    const { 
      reportType, 
      date, 
      weekStart, 
      weekEnd, 
      startDate, 
      endDate,
      subject,
      to,
      cc,
      boardId
    } = req.body;
    
    let tasks = [];
    
    // Fetch the board information for the report
    const boardsResponse = await taskModel.getAllBoards();
    const boards = boardsResponse.boards || [];
    const currentBoard = boards.find(board => board.id.toString() === boardId.toString()) || { id: boardId, name: 'Unknown Board' };
    
    if (reportType === 'daily') {
      tasks = await taskModel.getTasksByDate(date, boardId);
    } else if (reportType === 'weekly') {
      tasks = await taskModel.getTasksByDateRange(weekStart, weekEnd, boardId);
    } else if (reportType === 'custom') {
      tasks = await taskModel.getTasksByDateRange(startDate, endDate, boardId);
    }
    
    // Generate report content
    const reportContent = taskModel.generateReportContent(tasks, currentBoard);
    
    // Create email config
    const emailConfig = {
      subject,
      to: to.split(',').map(email => email.trim()),
      cc: cc ? cc.split(',').map(email => email.trim()) : []
    };
    
    res.render('reports/preview', {
      title: 'Report Preview',
      reportContent,
      emailConfig,
      tasks,
      taskCount: tasks.length,
      boards,
      currentBoardId: boardId,
      currentBoard
    });
  } catch (error) {
    res.status(500).render('error', { 
      title: 'Error',
      message: 'Failed to generate report',
      error: error.message 
    });
  }
};

exports.sendReport = async (req, res) => {
  try {
    const { subject, to, cc, reportContent, boardId } = req.body;
    const tasksJson = req.body.tasks;
    const tasks = JSON.parse(tasksJson);
    
    // Fetch board information if available
    let currentBoard = { name: 'Unknown Board' };
    if (boardId) {
      try {
        const boardsResponse = await taskModel.getAllBoards();
        const boards = boardsResponse.boards || [];
        currentBoard = boards.find(board => board.id.toString() === boardId.toString()) || { name: 'Unknown Board' };
      } catch (boardError) {
        console.error('Error fetching board details:', boardError.message);
      }
    }
    
    const emailConfig = {
      subject,
      to: to.split(',').map(email => email.trim()),
      cc: cc ? cc.split(',').map(email => email.trim()) : [],
      boardId,
      boardName: currentBoard.name
    };
    
    // Send the email
    const success = await taskModel.sendEmailReport(emailConfig, tasks);
    
    if (success) {
      res.render('reports/success', {
        title: 'Report Sent',
        message: 'Your report has been sent successfully!'
      });
    } else {
      throw new Error('Failed to send email');
    }
  } catch (error) {
    res.status(500).render('error', { 
      title: 'Error',
      message: 'Failed to send report',
      error: error.message 
    });
  }
};