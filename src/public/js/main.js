document.addEventListener('DOMContentLoaded', function() {
  // Task search functionality
  const taskSearchInput = document.getElementById('taskSearch');
  if (taskSearchInput) {
    taskSearchInput.addEventListener('keyup', function() {
      const searchValue = this.value.toLowerCase().trim();
      const tableRows = document.querySelectorAll('tbody tr');
      
      tableRows.forEach(row => {
        const text = row.textContent.toLowerCase();
        if (text.includes(searchValue) || searchValue === '') {
          row.style.display = '';
        } else {
          row.style.display = 'none';
        }
      });
    });
  }
  
  // Date range validation for custom report form
  const customReportForm = document.querySelector('form#reportForm[action="/reports/generate"]');
  if (customReportForm) {
    customReportForm.addEventListener('submit', function(e) {
      const startDateInput = document.getElementById('startDate');
      const endDateInput = document.getElementById('endDate');
      
      if (startDateInput && endDateInput) {
        const startDate = new Date(startDateInput.value);
        const endDate = new Date(endDateInput.value);
        
        if (startDate > endDate) {
          e.preventDefault();
          alert('Start date cannot be after end date');
          return false;
        }
      }
    });
  }
  
  // Weekly form date range validation
  const weeklyReportForm = document.querySelector('form#reportForm[action="/reports/generate"]');
  if (weeklyReportForm) {
    const weekStartInput = document.getElementById('weekStart');
    const weekEndInput = document.getElementById('weekEnd');
    
    if (weekStartInput && weekEndInput) {
      weekStartInput.addEventListener('change', function() {
        const weekStart = new Date(this.value);
        const weekEnd = new Date(weekEndInput.value);
        
        if (weekStart > weekEnd) {
          weekEndInput.value = this.value;
        }
      });
      
      weekEndInput.addEventListener('change', function() {
        const weekEnd = new Date(this.value);
        const weekStart = new Date(weekStartInput.value);
        
        if (weekEnd < weekStart) {
          alert('End date cannot be before start date');
          this.value = weekStartInput.value;
        }
      });
    }
  }
  
  // Initialize tooltips
  const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
  if (typeof bootstrap !== 'undefined') {
    tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl);
    });
  }
});
