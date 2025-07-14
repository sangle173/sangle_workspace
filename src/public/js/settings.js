document.addEventListener('DOMContentLoaded', function() {
  console.log('Settings page loaded');
  
  // API endpoint form validation
  const apiForm = document.querySelector('form[action="/settings/update-api"]');
  if (apiForm) {
    console.log('API settings form found');
    
    apiForm.addEventListener('submit', function(e) {
      try {
        const apiEndpointInput = document.getElementById('apiEndpoint');
        if (!apiEndpointInput || !apiEndpointInput.value) {
          e.preventDefault();
          alert('API Endpoint is required');
          return false;
        }
        
        // Validate URL
        try {
          new URL(apiEndpointInput.value);
        } catch (err) {
          e.preventDefault();
          alert('Please enter a valid URL');
          return false;
        }
        
        console.log('API form submission validated successfully');
        return true;
      } catch (error) {
        console.error('Error in settings form validation:', error);
        return true; // Let server handle validation if JS fails
      }
    });
  } else {
    console.warn('API settings form not found');
  }
});
