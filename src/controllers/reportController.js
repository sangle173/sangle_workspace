const taskModel = require('../models/taskModel');
const { format, startOfWeek, endOfWeek, addDays } = require('date-fns');

exports.getReportPage = async (req, res) => {
  try {
    // Set API base URL from session if available
    if (req.session.apiBaseUrl) {
      taskModel.setApiBaseUrl(req.session.apiBaseUrl);
    }
    
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
    
    // Generate report content in two formats
    const reportContent = taskModel.generateReportContent(tasks, currentBoard);
    const taskTable = taskModel.generateTaskTable(tasks);
    
    // Define consistent styles for all report elements
    const styles = {
      container: 'font-family: Calibri, sans-serif; font-size: 11pt; color: #000000; line-height: 1.15;',
      h1: 'font-family: Calibri, sans-serif; font-size: 16pt; font-weight: bold; color: #000000; margin-bottom: 6pt;',
      h2: 'font-family: Calibri, sans-serif; font-size: 14pt; font-weight: bold; color: #000000; margin-bottom: 6pt;',
      p: 'font-family: Calibri, sans-serif; font-size: 11pt; margin-bottom: 6pt;',
      strong: 'font-family: Calibri, sans-serif; font-weight: bold;',
      section: 'margin-top: 20pt; border-top: 1px solid #dddddd; padding-top: 12pt;'
    };
    
    // Build the period text based on report type
    let periodText = '';
    if (reportType === 'daily') {
      periodText = date;
    } else if (reportType === 'weekly') {
      periodText = `${weekStart} to ${weekEnd}`;
    } else {
      periodText = `${startDate} to ${endDate}`;
    }
    
    // Instead of trying to extract from the report content, let's recreate the team section directly
    // This ensures better control over formatting
    let teamSection = '';
    
    // Group tasks by team
    const tasksByTeam = {};
    tasks.forEach(task => {
      const teamName = task.team.name;
      if (!tasksByTeam[teamName]) {
        tasksByTeam[teamName] = [];
      }
      tasksByTeam[teamName].push(task);
    });
    
    // Build team section content
    Object.entries(tasksByTeam).forEach(([team, teamTasks]) => {
      teamSection += `
        <div style="margin-bottom: 10pt; font-family: Calibri, sans-serif;">
          <h3 style="font-family: Calibri, sans-serif; font-size: 12pt; font-weight: bold; margin-bottom: 5pt;">${team} (${teamTasks.length} tasks)</h3>
      `;        teamTasks.forEach(task => {
          let taskLinkHtml = '';
          if (task.link_to_result) {
            taskLinkHtml = ` - <a href="${task.link_to_result}" style="font-family: Calibri, sans-serif; font-size: 11pt; color: #0066cc; text-decoration: underline;">Link to Results</a>`;
          }
          
          teamSection += `
            <div style="margin-left: 10pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">
              <div style="font-family: Calibri, sans-serif; font-size: 11pt; font-weight: bold;">${task.jira_id}: ${task.jira_summary}${taskLinkHtml}</div>
              <div style="font-family: Calibri, sans-serif; font-size: 11pt; padding-left: 20pt;">Status: ${task.working_status.name} / ${task.ticket_status.name}</div>
              <div style="font-family: Calibri, sans-serif; font-size: 11pt; padding-left: 20pt;">Created: ${new Date(task.created_at).toLocaleString()}</div>
            </div>
          `;
        });
      
      teamSection += `</div>`;
    });
    
    // Combine both formats for full preview with consistent font styling
    const fullReportContent = `
      <div style="${styles.container}">
        <h1 style="${styles.h1}">Task Report for ${currentBoard.name}</h1>
        <p style="${styles.p}">Period: <span style="${styles.container}">${periodText}</span></p>
        <p style="${styles.p}">Total Tasks: <strong style="${styles.strong}">${tasks.length}</strong></p>
        
        ${taskTable}
        
        <div style="${styles.section}">
          <h2 style="${styles.h2}">
            Tasks by Team
          </h2>
          ${teamSection}
        </div>
      </div>
    `;
    
    // Create email config
    const emailConfig = {
      subject,
      to: to.split(',').map(email => email.trim()),
      cc: cc ? cc.split(',').map(email => email.trim()) : []
    };
    
    res.render('reports/preview', {
      title: 'Report Preview',
      reportContent: fullReportContent,
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