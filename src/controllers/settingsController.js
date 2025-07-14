const taskModel = require('../models/taskModel');

exports.getSettingsPage = async (req, res) => {
  try {
    // Ensure we have a valid session object
    if (!req.session) {
      console.error('Session object is undefined');
      req.session = {};
    }
    
    // Set API base URL from session if available
    if (req.session && req.session.apiBaseUrl) {
      taskModel.setApiBaseUrl(req.session.apiBaseUrl);
    }
    
    // Get the current API endpoint from the session or use default
    const currentApiEndpoint = req.session.apiBaseUrl || process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api';
    
    // Store success message temporarily
    const successMessage = req.session.success;
    
    // Clear flash messages before rendering
    if (req.session) {
      req.session.success = null;
    }
    
    res.render('settings', { 
      title: 'Settings',
      currentApiEndpoint,
      success: successMessage,
      error: null
    });
  } catch (error) {
    console.error('Error in getSettingsPage:', error);
    res.render('settings', { 
      title: 'Settings',
      currentApiEndpoint: req.session?.apiBaseUrl || process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api',
      success: null,
      error: error.message 
    });
  }
};

exports.updateApiEndpoint = async (req, res) => {
  try {
    // Set API base URL from session if available (for initial state)
    if (req.session && req.session.apiBaseUrl) {
      taskModel.setApiBaseUrl(req.session.apiBaseUrl);
    }
    
    const { apiEndpoint } = req.body;
    
    if (!apiEndpoint) {
      throw new Error('API Endpoint URL is required');
    }
    
    // Validate URL format
    try {
      new URL(apiEndpoint);
    } catch (e) {
      throw new Error('Invalid URL format. Please enter a valid URL');
    }
    
    // Update TaskModel with new API endpoint
    const success = taskModel.setApiBaseUrl(apiEndpoint);
    
    if (success) {
      // Save to session only if successful
      req.session.apiBaseUrl = apiEndpoint;
      req.session.success = 'API Endpoint updated successfully';
    } else {
      throw new Error('Failed to update API endpoint. Please check the URL format.');
    }
    
    res.redirect('/settings');
  } catch (error) {
    res.render('settings', {
      title: 'Settings',
      currentApiEndpoint: req.session.apiBaseUrl || process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api',
      success: null,
      error: error.message
    });
  }
};
