const axios = require('axios');

exports.checkApiStatus = async (req, res) => {
  try {
    const apiBaseUrl = process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api';
    const response = await axios.get(`${apiBaseUrl}/tasks?board_id=1`, { timeout: 5000 });
    
    if (response.status === 200) {
      res.json({
        status: 'success',
        message: 'API is accessible',
        apiUrl: apiBaseUrl,
        responseStatus: response.status,
        taskCount: response.data.data?.count || 0
      });
    } else {
      res.status(response.status).json({
        status: 'error',
        message: `API responded with status: ${response.status}`,
        apiUrl: apiBaseUrl
      });
    }
  } catch (error) {
    let errorMessage = 'Unknown error';
    let errorDetails = {};
    
    if (error.response) {
      // The request was made and the server responded with a status code
      errorMessage = `API responded with status: ${error.response.status}`;
      errorDetails = {
        status: error.response.status,
        data: error.response.data
      };
    } else if (error.request) {
      // The request was made but no response was received
      errorMessage = 'No response received from API server';
      errorDetails = {
        request: error.request._currentUrl
      };
    } else {
      // Something happened in setting up the request
      errorMessage = error.message;
    }
    
    res.status(500).json({
      status: 'error',
      message: errorMessage,
      apiUrl: process.env.API_BASE_URL || 'http://172.18.100.184/mini_project/public/api',
      details: errorDetails
    });
  }
};
