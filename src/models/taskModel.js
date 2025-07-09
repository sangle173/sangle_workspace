const axios = require('axios');
const { format, isWithinInterval } = require('date-fns');

class TaskModel {
  constructor() {
    this.apiBaseUrl = process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api';
  }

  async getAllTasks(boardId = process.env.DEFAULT_BOARD_ID || '1') {
    try {
      console.log(`Fetching tasks from: ${this.apiBaseUrl}/tasks?board_id=${boardId}`);
      const response = await axios.get(`${this.apiBaseUrl}/tasks?board_id=${boardId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching tasks:', error.message);
      console.error('API URL:', `${this.apiBaseUrl}/tasks?board_id=${boardId}`);
      
      if (error.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        console.error('API Response:', error.response.data);
        console.error('Status:', error.response.status);
        throw new Error(`API Error: ${error.response.status} - ${JSON.stringify(error.response.data)}`);
      } else if (error.request) {
        // The request was made but no response was received
        throw new Error('No response received from API server');
      } else {
        // Something happened in setting up the request
        throw new Error(`Error setting up request: ${error.message}`);
      }
    }
  }

  async getTasksByDate(date, boardId = process.env.DEFAULT_BOARD_ID) {
    try {
      const allTasks = await this.getAllTasks(boardId);
      const formattedDate = format(new Date(date), 'yyyy-MM-dd');
      
      return allTasks.data.tasks.filter(task => 
        task.created_at.startsWith(formattedDate)
      );
    } catch (error) {
      console.error('Error fetching tasks by date:', error.message);
      throw new Error('Failed to fetch tasks by date');
    }
  }

  async getTasksByDateRange(startDate, endDate, boardId = process.env.DEFAULT_BOARD_ID) {
    try {
      const allTasks = await this.getAllTasks(boardId);
      const start = new Date(startDate).getTime();
      const end = new Date(endDate).getTime();
      
      return allTasks.data.tasks.filter(task => {
        const taskDate = new Date(task.created_at).getTime();
        return taskDate >= start && taskDate <= end;
      });
    } catch (error) {
      console.error('Error fetching tasks by date range:', error.message);
      throw new Error('Failed to fetch tasks by date range');
    }
  }

  generateReportContent(tasks, board = { name: 'All Boards' }) {
    if (!tasks || tasks.length === 0) {
      return `No tasks found for the selected period in ${board.name}.`;
    }

    // Group tasks by team
    const tasksByTeam = {};
    tasks.forEach(task => {
      const teamName = task.team.name;
      if (!tasksByTeam[teamName]) {
        tasksByTeam[teamName] = [];
      }
      tasksByTeam[teamName].push(task);
    });

    // Build report content
    let content = `# Task Report for ${board.name}\n\n`;
    content += `Total Tasks: ${tasks.length}\n\n`;

    Object.entries(tasksByTeam).forEach(([team, teamTasks]) => {
      content += `## ${team} (${teamTasks.length} tasks)\n\n`;
      
      teamTasks.forEach(task => {
        content += `* ${task.jira_id}: ${task.jira_summary}\n`;
        content += `  - Status: ${task.working_status.name} / ${task.ticket_status.name}\n`;
        content += `  - Created: ${new Date(task.created_at).toLocaleString()}\n`;
        if (task.link_to_result) {
          content += `  - Link to Results: ${task.link_to_result}\n`;
        }
        content += `\n`;
      });
    });

    return content;
  }

  async sendEmailReport(emailConfig, tasks) {
    // In a real application, this would send an email using nodemailer
    // For now, we'll just simulate it
    console.log('Email would be sent with config:', emailConfig);
    console.log('Report content would include', tasks.length, 'tasks');
    return true;
  }

  async getAllBoards() {
    try {
      console.log(`Fetching boards from: ${this.apiBaseUrl}/boards`);
      const response = await axios.get(`${this.apiBaseUrl}/boards`);
      return response.data;
    } catch (error) {
      console.error('Error fetching boards:', error.message);
      
      if (error.response) {
        console.error('API Response:', error.response.data);
        console.error('Status:', error.response.status);
        throw new Error(`API Error: ${error.response.status} - ${JSON.stringify(error.response.data)}`);
      } else if (error.request) {
        throw new Error('No response received from API server');
      } else {
        throw new Error(`Error setting up request: ${error.message}`);
      }
    }
  }
}

module.exports = new TaskModel();
