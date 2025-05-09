/**
 * Script to check and help set up environment variables
 */
class EnvChecker {
    constructor() {
        this.statusElement = document.getElementById('env-status');
        this.setupInstructionsElement = document.getElementById('setup-instructions');
        this.envNoteElement = document.getElementById('env-note');
    }

    async checkEnvStatus() {
        try {
            const response = await fetch('/api/env/status');
            const data = await response.json();
            
            if (data.status === 'ok') {
                this.showSuccessStatus();
            } else {
                this.showErrorStatus(data.message);
            }
        } catch (error) {
            console.error('Error checking environment status:', error);
            this.showErrorStatus('Could not check environment status');
        }
    }

    showSuccessStatus() {
        // For a clean workspace, we hide both the status element and setup instructions
        if (this.statusElement) {
            // Instead of showing success message, just hide the element completely
            this.statusElement.style.display = 'none';
        }
        
        if (this.setupInstructionsElement) {
            this.setupInstructionsElement.style.display = 'none';
        }
        
        // Also hide the environment note in the form card
        if (this.envNoteElement) {
            this.envNoteElement.style.display = 'none';
        }
    }

    showErrorStatus(message) {
        if (this.statusElement) {
            this.statusElement.innerHTML = `
                <div class="alert alert-warning" role="alert">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    ${message}
                </div>
            `;
            this.statusElement.style.display = 'block';
        }
        
        if (this.setupInstructionsElement) {
            this.setupInstructionsElement.style.display = 'block';
        }
        
        // Show the environment note in the form card
        if (this.envNoteElement) {
            this.envNoteElement.style.display = 'block';
        }
    }
}

// Initialize checker when page loads
document.addEventListener('DOMContentLoaded', () => {
    const envChecker = new EnvChecker();
    
    // Only run if we're on a page that needs environment variables
    if (document.getElementById('env-status')) {
        envChecker.checkEnvStatus();
    }
}); 