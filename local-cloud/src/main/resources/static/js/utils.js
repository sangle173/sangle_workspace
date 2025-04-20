/**
 * Utility functions for handling file uploads and attachments
 */

// Format a file size in bytes to a human-readable string
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Get file type icon based on MIME type or extension
function getFileIcon(fileType, fileName) {
    if (!fileType) {
        // Try to determine from file extension
        const ext = fileName.split('.').pop().toLowerCase();
        if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) {
            return 'bi-file-earmark-image';
        } else if (['mp4', 'webm', 'ogg', 'avi', 'mov'].includes(ext)) {
            return 'bi-file-earmark-play';
        } else if (['pdf'].includes(ext)) {
            return 'bi-file-earmark-pdf';
        } else if (['doc', 'docx'].includes(ext)) {
            return 'bi-file-earmark-word';
        } else if (['xls', 'xlsx'].includes(ext)) {
            return 'bi-file-earmark-excel';
        } else {
            return 'bi-file-earmark';
        }
    }
    
    // Determine from MIME type
    if (fileType.startsWith('image/')) {
        return 'bi-file-earmark-image';
    } else if (fileType.startsWith('video/')) {
        return 'bi-file-earmark-play';
    } else if (fileType.includes('pdf')) {
        return 'bi-file-earmark-pdf';
    } else if (fileType.includes('word') || fileType.includes('document')) {
        return 'bi-file-earmark-word';
    } else if (fileType.includes('excel') || fileType.includes('spreadsheet')) {
        return 'bi-file-earmark-excel';
    } else {
        return 'bi-file-earmark';
    }
}

// Create a debug log function that logs to console in development mode
function debugLog(...args) {
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        console.log(...args);
    }
} 