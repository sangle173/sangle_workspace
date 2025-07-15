const axios = require('axios');
const { format, isWithinInterval } = require('date-fns');

// Unicode symbols for status labels
const STATUS_ICONS = {
  'Done': '✅',         // Unicode for Done
  'In-progress': '⏳' // Unicode for In Progress
};

class TaskModel {
  constructor() {
    this.apiBaseUrl = process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api';
  }
  
  setApiBaseUrl(url) {
    if (url) {
      try {
        // Validate URL
        new URL(url);
        this.apiBaseUrl = url;
        console.log(`API Base URL updated to: ${url}`);
        return true;
      } catch (error) {
        console.error(`Invalid API URL format: ${url}`);
        console.error(error);
        return false;
      }
    }
    return false;
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
  
  calculateTaskSummary(tasks) {
    // Initialize summary object
    const summary = {
      totalTasks: tasks.length,
      byType: {},
      byStatus: {},
      byTeam: {},
      teamBreakdown: {}
    };
    
    // Count tasks by type
    tasks.forEach(task => {
      // Count by type
      const typeName = task.type.name || 'Other';
      if (!summary.byType[typeName]) {
        summary.byType[typeName] = 0;
      }
      summary.byType[typeName]++;
      
      // Count by status
      const statusName = task.ticket_status.name || 'Other';
      if (!summary.byStatus[statusName]) {
        summary.byStatus[statusName] = 0;
      }
      summary.byStatus[statusName]++;
      
      // Count by team
      const teamName = task.team.name || 'Unassigned';
      if (!summary.byTeam[teamName]) {
        summary.byTeam[teamName] = 0;
      }
      summary.byTeam[teamName]++;
      
      // Create team breakdown data
      if (!summary.teamBreakdown[teamName]) {
        summary.teamBreakdown[teamName] = {};
      }
      
      // Count team tasks by type and working status
      if (!summary.teamBreakdown[teamName][typeName]) {
        summary.teamBreakdown[teamName][typeName] = {
          'Done': 0,
          'In-progress': 0
        };
      }
      
      // Map working status to simplified "Done" or "In-progress" categories
      const workStatus = task.working_status.name || 'Other';
      const simplifiedStatus = workStatus.toLowerCase().includes('done') || 
                              workStatus.toLowerCase().includes('complete') ? 
                              'Done' : 'In-progress';
      
      summary.teamBreakdown[teamName][typeName][simplifiedStatus]++;
    });
    
    return summary;
  }

  generateReportContent(tasks, board = { name: 'All Boards' }) {
    if (!tasks || tasks.length === 0) {
      return `No tasks found for the selected period in ${board.name}.`;
    }

    // Calculate summary statistics
    const summary = this.calculateTaskSummary(tasks);

    // Define consistent styles for reuse
    const styles = {
      container: 'font-family: Calibri, sans-serif; font-size: 11pt; color: #000000; line-height: 1.3;',
      h1: 'font-family: Calibri, sans-serif; font-size: 16pt; font-weight: bold; color: #000000; margin-bottom: 10pt; text-align: left !important;',
      h2: 'font-family: Calibri, sans-serif; font-size: 14pt; font-weight: bold; color: #0066cc; margin-top: 15pt; margin-bottom: 10pt;',
      h3: 'font-family: Calibri, sans-serif; font-size: 12pt; font-weight: bold; margin-top: 10pt; margin-bottom: 5pt; margin-left: 20pt;',
      h4: 'font-family: Calibri, sans-serif; font-size: 11pt; font-weight: bold; margin-top: 8pt; margin-bottom: 5pt; margin-left: 40pt;',
      p: 'font-family: Calibri, sans-serif; font-size: 11pt; margin-bottom: 6pt;',
      taskTitle: 'font-family: Calibri, sans-serif; font-size: 11pt; font-weight: normal;',
      link: 'font-family: Calibri, sans-serif; font-size: 11pt; color: #0066cc; text-decoration: underline;',
      table: 'width: 66%; border-collapse: collapse; margin-bottom: 20pt; font-family: Calibri, sans-serif;',
      th: 'background-color: #f0f0f0; padding: 8pt; text-align: left; border: 1px solid #ddd; font-weight: bold;',
      td: 'padding: 8pt; text-align: left; border: 1px solid #ddd;',
      summaryBox: 'display: inline-block; margin: 5pt; padding: 10pt; background-color: #f9f9f9; border: 1px solid #ddd; border-radius: 5px; min-width: 120px; text-align: center;',
      summaryNumber: 'font-size: 18pt; font-weight: bold; color: #0066cc; margin: 5pt 0;',
      summaryLabel: 'font-size: 10pt; color: #666;'
    };
    
    let content = `
      <div style="${styles.container}">
        <div style="text-align: left !important;">
          <h1 style="${styles.h1}; text-align: left !important;">Task Report Summary</h1>
        </div>
        
        <!-- Summary Section - Card Style -->
        <div style="margin-bottom: 20pt; text-align: center;">
          <div style="${styles.summaryBox}">
            <div style="${styles.summaryNumber}">${summary.totalTasks}</div>
            <div style="${styles.summaryLabel}">Total Tasks</div>
          </div>
          
          <!-- Add more summary cards for top types -->
          ${Object.keys(summary.byType).length > 0 ? 
            Object.entries(summary.byType)
              .sort((a, b) => b[1] - a[1])
              .slice(0, 3)
              .map(([type, count]) => 
                `<div style="${styles.summaryBox}">
                  <div style="${styles.summaryNumber}">${count}</div>
                  <div style="${styles.summaryLabel}">${type}</div>
                </div>`
              ).join('') 
            : ''}
        </div>
        
        <!-- Team Breakdown Matrix Table -->
        <h2 style="${styles.h2}">Team Task Matrix</h2>
        <table style="${styles.table}; background-color: #e9f1fb;">
          <tr>
            <th style="${styles.th}" rowspan="2">Team</th>
            <th style="${styles.th}" colspan="2">Testing requests</th>
            <th style="${styles.th}" colspan="2">Tickets verification</th>
            <th style="${styles.th}" rowspan="2">Bugs reported</th>
          </tr>
          <tr>
            <th style="${styles.th}">Done</th>
            <th style="${styles.th}">In-progress</th>
            <th style="${styles.th}">Done</th>
            <th style="${styles.th}">In-progress</th>
          </tr>
          
          ${Object.entries(summary.teamBreakdown)
            .map(([teamName, teamTypes]) => `
              <tr>
                <td style="${styles.td}">${teamName}</td>
                <td style="${styles.td}">${teamTypes['Testing Requests']?.Done || 0}</td>
                <td style="${styles.td}">${teamTypes['Testing Requests']?.['In-progress'] || 0}</td>
                <td style="${styles.td}">${teamTypes['Tickets Verification']?.Done || 0}</td>
                <td style="${styles.td}">${teamTypes['Tickets Verification']?.['In-progress'] || 0}</td>
                <td style="${styles.td}">${teamTypes['Bugs Reported'] ? (teamTypes['Bugs Reported'].Done || 0) + (teamTypes['Bugs Reported']['In-progress'] || 0) : 0}</td>
              </tr>
            `).join('')
          }
        </table>
      </div>
      
      <h1 style="${styles.h1}">Details of the assignment:</h1>
    `;
    
    // First group by team, then by type, then by working status
    const tasksByTeam = {};
    tasks.forEach(task => {
      // Group by team name
      const teamName = task.team.name || 'Unassigned';
      if (!tasksByTeam[teamName]) {
        tasksByTeam[teamName] = {
          tasks: [],
          byType: {}
        };
      }
      
      // Add to team tasks array
      tasksByTeam[teamName].tasks.push(task);
      
      // Group by type within each team
      const typeName = task.type.name || 'Other';
      if (!tasksByTeam[teamName].byType[typeName]) {
        tasksByTeam[teamName].byType[typeName] = {
          tasks: [],
          byWorkingStatus: {}
        };
      }
      
      // Add to type tasks array
      tasksByTeam[teamName].byType[typeName].tasks.push(task);
      
      // Group by working status within each type
      const workingStatus = task.working_status.name || 'Other';
      if (!tasksByTeam[teamName].byType[typeName].byWorkingStatus[workingStatus]) {
        tasksByTeam[teamName].byType[typeName].byWorkingStatus[workingStatus] = [];
      }
      
      // Add to working status tasks array
      tasksByTeam[teamName].byType[typeName].byWorkingStatus[workingStatus].push(task);
    });
    
    // Build hierarchical view: Team -> Type -> Working Status -> Tickets
    Object.entries(tasksByTeam).forEach(([team, teamData]) => {
      content += `
        <h2 style="${styles.h2}">${team}</h2>
      `;
      
      // Define the order of types
      const typeOrder = ["Testing Requests", "Tickets Verification", "Bugs Reported"];
      
      // Process types in the specified order first
      typeOrder.forEach(orderedType => {
        if (teamData.byType[orderedType]) {
          const typeData = teamData.byType[orderedType];
          // For "Bugs Reported" type, show bug count in header
          if (orderedType === "Bugs Reported") {
            // Flatten all tasks from all working statuses
            const allTasksForType = [];
            Object.entries(typeData.byWorkingStatus).forEach(([_, workingStatusTasks]) => {
              allTasksForType.push(...workingStatusTasks);
            });
            const bugCount = allTasksForType.length;
            content += `
              <h3 style="${styles.h3}">Bug found (${bugCount}):</h3>
            `;
            // Display all tasks without working status headers
            allTasksForType.forEach(task => {
              // Create Jira hyperlink
              const jiraUrl = `https://jira.sonos.com/browse/${task.jira_id}`;
              
              content += `
                <div style="margin-left: 60pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">
                  • <a href="${jiraUrl}" style="${styles.link}" target="_blank">${task.jira_id}</a> - 
                  ${task.jira_summary}
              `;
              
              if (task.link_to_result) {
                content += ` - <a href="${task.link_to_result}" style="${styles.link}" target="_blank">Link to Results</a>`;
              }
              
              content += `</div>`;
            });
          } else {
            content += `
              <h3 style="${styles.h3}">${orderedType}</h3>
            `;
            
            // For other types, show working status as before
            Object.entries(typeData.byWorkingStatus)
              .sort(([a], [b]) => {
                // Sort: Done first, then In-progress, then others
                const normalize = s => s.replace(/[-_ ]/gi, '').toLowerCase();
                if (normalize(a) === 'done') return -1;
                if (normalize(b) === 'done') return 1;
                if (normalize(a).includes('inprogress')) return -1;
                if (normalize(b).includes('inprogress')) return 1;
                return a.localeCompare(b);
              })
              .forEach(([workingStatus, workingStatusTasks]) => {
                // Show icon only next to the working status label
                let statusLabel = workingStatus;
                if (workingStatus === 'Done') {
                  statusLabel = `${STATUS_ICONS['Done']} Done`;
                } else if (workingStatus.replace(/[-_ ]/gi, '').toLowerCase().includes('inprogress')) {
                  statusLabel = `${STATUS_ICONS['In-progress']} In Progress`;
                }
                content += `
                  <h4 style="${styles.h4}">${statusLabel}</h4>
                `;
                
                // Display tasks
                workingStatusTasks.forEach(task => {
                  // Create Jira hyperlink
                  const jiraUrl = `https://jira.sonos.com/browse/${task.jira_id}`;
                  
                  // Use the ticket status color directly from the desc field
                  const statusColor = task.ticket_status.desc || '#000000';
                  const statusStyle = `color: ${statusColor}; font-weight: bold;`;
                  
                  content += `
                    <div style="margin-left: 60pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">
                      • <a href="${jiraUrl}" style="${styles.link}" target="_blank">${task.jira_id}</a> - 
                      ${task.jira_summary} - <span style="${statusStyle}">${task.ticket_status.name}</span>
                  `;
                  
                  if (task.link_to_result) {
                    content += ` - <a href="${task.link_to_result}" style="${styles.link}" target="_blank">Link to Results</a>`;
                  }
                  
                  content += `</div>`;
                });
              });
          }
          
          // Remove from byType so we don't process it again in the next loop
          delete teamData.byType[orderedType];
        }
      });
      
      // Process any remaining types (not in the specified order)
      Object.entries(teamData.byType).forEach(([typeName, typeData]) => {
        // Custom header for Bugs Reported
        if (typeName === "Bugs Reported") {
          const allTasksForType = [];
          Object.entries(typeData.byWorkingStatus).forEach(([_, workingStatusTasks]) => {
            allTasksForType.push(...workingStatusTasks);
          });
          content += `
            <h3 style="${styles.h3}">Bug found (${allTasksForType.length}):</h3>
          `;
          allTasksForType.forEach(task => {
            // Create Jira hyperlink
            const jiraUrl = `https://jira.sonos.com/browse/${task.jira_id}`;
            
            // Use the ticket status color directly from the desc field
            const statusColor = task.ticket_status.desc || '#000000';
            const statusStyle = `color: ${statusColor}; font-weight: bold;`;              content += `
              <div style="margin-left: 60pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">
                • <a href="${jiraUrl}" style="${styles.link}" target="_blank">${task.jira_id}</a> - 
                ${task.jira_summary} - <span style="${statusStyle}">${task.ticket_status.name}</span>
            `;
            
            if (task.link_to_result) {
              content += ` - <a href="${task.link_to_result}" style="${styles.link}" target="_blank">Link to Results</a>`;
            }
            
            content += `</div>`;
          });
        } else {
          content += `
            <h3 style="${styles.h3}">${typeName}</h3>
          `;
          
          // For each working status within the type
          Object.entries(typeData.byWorkingStatus).forEach(([workingStatus, workingStatusTasks]) => {
            content += `
              <h4 style="${styles.h4}">${workingStatus}</h4>
            `;
            
            // Display tasks
            workingStatusTasks.forEach(task => {
              // Create Jira hyperlink
              const jiraUrl = `https://jira.sonos.com/browse/${task.jira_id}`;
              
              // Use the ticket status color directly from the desc field
              const statusColor = task.ticket_status.desc || '#000000';
              const statusStyle = `color: ${statusColor}; font-weight: bold;`;              content += `
                <div style="margin-left: 60pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">
                  • <a href="${jiraUrl}" style="${styles.link}" target="_blank">${task.jira_id}</a> - 
                  ${task.jira_summary} - <span style="${statusStyle}">${task.ticket_status.name}</span>
              `;
              
              if (task.link_to_result) {
                content += ` - <a href="${task.link_to_result}" style="${styles.link}" target="_blank">Link to Results</a>`;
              }
              
              content += `</div>`;
            });
          });
        }
      });
    });
    
    content += `</div>`;
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
  
  // Generate a plain table for email
  generateTaskTable(tasks) {
    // Define consistent font style for Outlook
    const fontStyle = 'font-family: Calibri, sans-serif; font-size: 11pt;';
    
    if (!tasks || tasks.length === 0) {
      return `<p style="${fontStyle}">No tasks found.</p>`;
    }

    // Calculate summary statistics
    const summary = this.calculateTaskSummary(tasks);
    
    // Define consistent styles
    const styles = {
      container: 'font-family: Calibri, sans-serif; font-size: 11pt; color: #000000; line-height: 1.3;',
      h1: 'font-family: Calibri, sans-serif; font-size: 16pt; font-weight: bold; color: #000000; margin-bottom: 10pt; text-align: left !important;',
      h2: 'font-family: Calibri, sans-serif; font-size: 14pt; font-weight: bold; color: #0066cc; margin-top: 15pt; margin-bottom: 10pt;',
      h3: 'font-family: Calibri, sans-serif; font-size: 12pt; font-weight: bold; margin-top: 10pt; margin-bottom: 5pt; margin-left: 20pt;',
      h4: 'font-family: Calibri, sans-serif; font-size: 11pt; font-weight: bold; margin-top: 8pt; margin-bottom: 5pt; margin-left: 40pt;',
      link: 'color: #0066cc; text-decoration: underline;',
      table: 'width: 66%; border-collapse: collapse; margin-bottom: 20pt; font-family: Calibri, sans-serif;',
      th: 'background-color: #f0f0f0; padding: 8pt; text-align: left; border: 1px solid #ddd; font-weight: bold;',
      td: 'padding: 8pt; text-align: left; border: 1px solid #ddd;',
      summaryBox: 'display: inline-block; margin: 5pt; padding: 10pt; background-color: #f9f9f9; border: 1px solid #ddd; border-radius: 5px; min-width: 120px; text-align: center;',
      summaryNumber: 'font-size: 18pt; font-weight: bold; color: #0066cc; margin: 5pt 0;',
      summaryLabel: 'font-size: 10pt; color: #666;'
    };
    
    // First group by team, then by type, then by working status
    const tasksByTeam = {};
    tasks.forEach(task => {
      // Group by team name
      const teamName = task.team.name || 'Unassigned';
      if (!tasksByTeam[teamName]) {
        tasksByTeam[teamName] = {
          tasks: [],
          byType: {}
        };
      }
      
      // Add to team tasks array
      tasksByTeam[teamName].tasks.push(task);
      
      // Group by type within each team
      const typeName = task.type.name || 'Other';
      if (!tasksByTeam[teamName].byType[typeName]) {
        tasksByTeam[teamName].byType[typeName] = {
          tasks: [],
          byWorkingStatus: {}
        };
      }
      
      // Add to type tasks array
      tasksByTeam[teamName].byType[typeName].tasks.push(task);
      
      // Group by working status within each type
      const workingStatus = task.working_status.name || 'Other';
      if (!tasksByTeam[teamName].byType[typeName].byWorkingStatus[workingStatus]) {
        tasksByTeam[teamName].byType[typeName].byWorkingStatus[workingStatus] = [];
      }
      
      // Add to working status tasks array
      tasksByTeam[teamName].byType[typeName].byWorkingStatus[workingStatus].push(task);
    });
    
    let tableHtml = `
      <div style="${styles.container}">
        <div style="text-align: left !important;">
          <h1 style="${styles.h1}; text-align: left !important;">Task Report Summary</h1>
        </div>
        
        <!-- Summary Section - Card Style -->
        <div style="margin-bottom: 20pt; text-align: center;">
          <div style="${styles.summaryBox}">
            <div style="${styles.summaryNumber}">${summary.totalTasks}</div>
            <div style="${styles.summaryLabel}">Total Tasks</div>
          </div>
          
          <!-- Add more summary cards for top types -->
          ${Object.keys(summary.byType).length > 0 ? 
            Object.entries(summary.byType)
              .sort((a, b) => b[1] - a[1])
              .slice(0, 3)
              .map(([type, count]) => 
                `<div style="${styles.summaryBox}">
                  <div style="${styles.summaryNumber}">${count}</div>
                  <div style="${styles.summaryLabel}">${type}</div>
                </div>`
              ).join('') 
            : ''}
        </div>
        
        <!-- Team Breakdown Matrix Table -->
        <h2 style="${styles.h2}">Team Task Matrix</h2>
        <table style="${styles.table}; background-color: #e9f1fb;">
          <tr>
            <th style="${styles.th}" rowspan="2">Team</th>
            <th style="${styles.th}" colspan="2">Testing requests</th>
            <th style="${styles.th}" colspan="2">Tickets verification</th>
            <th style="${styles.th}" rowspan="2">Bugs reported</th>
          </tr>
          <tr>
            <th style="${styles.th}">Done</th>
            <th style="${styles.th}">In-progress</th>
            <th style="${styles.th}">Done</th>
            <th style="${styles.th}">In-progress</th>
          </tr>
          
          ${Object.entries(summary.teamBreakdown)
            .map(([teamName, teamTypes]) => `
              <tr>
                <td style="${styles.td}">${teamName}</td>
                <td style="${styles.td}">${teamTypes['Testing Requests']?.Done || 0}</td>
                <td style="${styles.td}">${teamTypes['Testing Requests']?.['In-progress'] || 0}</td>
                <td style="${styles.td}">${teamTypes['Tickets Verification']?.Done || 0}</td>
                <td style="${styles.td}">${teamTypes['Tickets Verification']?.['In-progress'] || 0}</td>
                <td style="${styles.td}">${teamTypes['Bugs Reported'] ? (teamTypes['Bugs Reported'].Done || 0) + (teamTypes['Bugs Reported']['In-progress'] || 0) : 0}</td>
              </tr>
            `).join('')
          }
        </table>
      </div>
      
      <h1 style="${styles.h1}">Details of the assignment:</h1>
    `;
    
    // Build hierarchical view: Team -> Type -> Working Status -> Tickets
    Object.entries(tasksByTeam).forEach(([team, teamData]) => {
      tableHtml += `
        <h2 style="${styles.h2}">${team}</h2>
      `;
      // Define the order of types
      const typeOrder = ["Testing Requests", "Tickets Verification", "Bugs Reported"];
      // Process types in the specified order first
      typeOrder.forEach(orderedType => {
        if (teamData.byType[orderedType]) {
          const typeData = teamData.byType[orderedType];
          // For "Bugs Reported" type, show bug count in header
          if (orderedType === "Bugs Reported") {
            const allTasksForType = [];
            Object.entries(typeData.byWorkingStatus).forEach(([_, workingStatusTasks]) => {
              allTasksForType.push(...workingStatusTasks);
            });
            const bugCount = allTasksForType.length;
            tableHtml += `
              <h3 style="${styles.h3}">Bug found (${bugCount}):</h3>
            `;
            allTasksForType.forEach(task => {
              // Create Jira hyperlink
              const jiraUrl = `https://jira.sonos.com/browse/${task.jira_id}`;
              
              tableHtml += `
                <div style="margin-left: 60pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">
                  • <a href="${jiraUrl}" style="${styles.link}" target="_blank">${task.jira_id}</a> - 
                  ${task.jira_summary}
              `;
              
              if (task.link_to_result) {
                tableHtml += ` - <a href="${task.link_to_result}" style="${styles.link}" target="_blank">Link to Results</a>`;
              }
              
              tableHtml += `</div>`;
            });
          } else {
            tableHtml += `
              <h3 style="${styles.h3}">${orderedType}</h3>
            `;
            
            // For other types, show working status as before
            Object.entries(typeData.byWorkingStatus)
              .sort(([a], [b]) => {
                // Sort: Done first, then In-progress, then others
                const normalize = s => s.replace(/[-_ ]/gi, '').toLowerCase();
                if (normalize(a) === 'done') return -1;
                if (normalize(b) === 'done') return 1;
                if (normalize(a).includes('inprogress')) return -1;
                if (normalize(b).includes('inprogress')) return 1;
                return a.localeCompare(b);
              })
              .forEach(([workingStatus, workingStatusTasks]) => {
                let statusLabel = workingStatus;
                if (workingStatus === 'Done') {
                  statusLabel = `${STATUS_ICONS['Done']} Done`;
                } else if (workingStatus.replace(/[-_ ]/gi, '').toLowerCase().includes('inprogress')) {
                  statusLabel = `${STATUS_ICONS['In-progress']} In Progress`;
                }
                tableHtml += `
                  <h4 style="${styles.h4}">${statusLabel}</h4>
                `;
                
                // Display tasks
                workingStatusTasks.forEach(task => {
                  // Create Jira hyperlink
                  const jiraUrl = `https://jira.sonos.com/browse/${task.jira_id}`;
                  
                  // Use the ticket status color directly from the desc field
                  const statusColor = task.ticket_status.desc || '#000000';
                  const statusStyle = `color: ${statusColor}; font-weight: bold;`;
                  
                  tableHtml += `
                    <div style="margin-left: 60pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">• <a href="${jiraUrl}" style="${styles.link}" target="_blank">${task.jira_id}</a> - 
                    ${task.jira_summary} - <span style="${statusStyle}">${task.ticket_status.name}</span>
                  `;
                  
                  if (task.link_to_result) {
                    tableHtml += ` - <a href="${task.link_to_result}" style="${styles.link}" target="_blank">Link to Results</a>`;
                  }
                  
                  tableHtml += `</div>`;
                });
              });
          }
          
          // Remove from byType so we don't process it again in the next loop
          delete teamData.byType[orderedType];
        }
      });
      // Process any remaining types (not in the specified order)
      Object.entries(teamData.byType).forEach(([typeName, typeData]) => {
        // Custom header for Bugs Reported
        if (typeName === "Bugs Reported") {
          const allTasksForType = [];
          Object.entries(typeData.byWorkingStatus).forEach(([_, workingStatusTasks]) => {
            allTasksForType.push(...workingStatusTasks);
          });
          tableHtml += `
            <h3 style="${styles.h3}">Bug found (${allTasksForType.length}):</h3>
          `;
          allTasksForType.forEach(task => {
            // Create Jira hyperlink
            const jiraUrl = `https://jira.sonos.com/browse/${task.jira_id}`;
            
            // Use the ticket status color directly from the desc field
            const statusColor = task.ticket_status.desc || '#000000';
            const statusStyle = `color: ${statusColor}; font-weight: bold;`;              tableHtml += `
              <div style="margin-left: 60pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">
                • <a href="${jiraUrl}" style="${styles.link}" target="_blank">${task.jira_id}</a> - 
                ${task.jira_summary} - <span style="${statusStyle}">${task.ticket_status.name}</span>
            `;
            
            if (task.link_to_result) {
              tableHtml += ` - <a href="${task.link_to_result}" style="${styles.link}" target="_blank">Link to Results</a>`;
            }
            
            tableHtml += `</div>`;
          });
        } else {
          tableHtml += `
            <h3 style="${styles.h3}">${typeName}</h3>
          `;
          
          // For each working status within the type
          Object.entries(typeData.byWorkingStatus).forEach(([workingStatus, workingStatusTasks]) => {
            let statusLabel = workingStatus;
            if (workingStatus === 'Done') {
              statusLabel = `${STATUS_ICONS['Done']} Done`;
            } else if (workingStatus.replace(/[-_ ]/gi, '').toLowerCase().includes('inprogress')) {
              statusLabel = `${STATUS_ICONS['In-progress']} In Progress`;
            }
            tableHtml += `
              <h4 style="${styles.h4}">${statusLabel}</h4>
            `;
            
            // Display tasks
            workingStatusTasks.forEach(task => {
              // Create Jira hyperlink
              const jiraUrl = `https://jira.sonos.com/browse/${task.jira_id}`;
              
              // Use the ticket status color directly from the desc field
              const statusColor = task.ticket_status.desc || '#000000';
              const statusStyle = `color: ${statusColor}; font-weight: bold;`;              tableHtml += `
                <div style="margin-left: 60pt; margin-bottom: 8pt; font-family: Calibri, sans-serif;">• <a href="${jiraUrl}" style="${styles.link}" target="_blank">${task.jira_id}</a> - 
                ${task.jira_summary} - <span style="${statusStyle}">${task.ticket_status.name}</span>
              `;
              
              if (task.link_to_result) {
                tableHtml += ` - <a href="${task.link_to_result}" style="${styles.link}" target="_blank">Link to Results</a>`;
              }
              
              tableHtml += `</div>`;
            });
          });
        }
      });
    });
    
    tableHtml += `</div>`;
    return tableHtml;
  }
}

module.exports = new TaskModel();
