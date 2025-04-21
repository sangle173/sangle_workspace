/**
 * Utility functions for handling file uploads and attachments
 */

// Format a file size in bytes to a human-readable string
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
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

// Image and video utility functions
const imageConfig = {
    resizeUnit: '%',
    resizeOptions: [
        {
            name: 'imageResize:original',
            value: null,
            label: 'Original'
        },
        {
            name: 'imageResize:25',
            value: '25',
            label: '25%'
        },
        {
            name: 'imageResize:50',
            value: '50',
            label: '50%'
        },
        {
            name: 'imageResize:75',
            value: '75',
            label: '75%'
        }
    ],
    styles: {
        options: [
            'alignLeft',
            'alignCenter',
            'alignRight'
        ]
    }
};

const videoConfig = {
    styles: [
        { name: 'video-small', title: 'Small', className: 'video-small' },
        { name: 'video-medium', title: 'Medium', className: 'video-medium' },
        { name: 'video-large', title: 'Large', className: 'video-large' }
    ]
};

// File type utilities
const fileTypeUtils = {
    isImage: function(file) {
        return file.type.startsWith('image/');
    },
    
    isVideo: function(file) {
        return file.type.startsWith('video/');
    },
    
    isAudio: function(file) {
        return file.type.startsWith('audio/');
    },
    
    isPdf: function(file) {
        return file.type === 'application/pdf';
    },
    
    getFileIcon: function(file) {
        if (this.isImage(file)) return 'bi-file-image';
        if (this.isVideo(file)) return 'bi-file-play';
        if (this.isAudio(file)) return 'bi-file-music';
        if (this.isPdf(file)) return 'bi-file-pdf';
        return 'bi-file-earmark';
    },
    
    formatFileSize: function(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
};

// CKEditor utilities
function insertContentIntoCKEditor(editor, content) {
    const viewFragment = editor.data.processor.toView(content);
    const modelFragment = editor.data.toModel(viewFragment);
    editor.model.insertContent(modelFragment);
}

// Create HTML elements for editor insertion
function createImageElement(url, alt = '') {
    return `<figure class="image"><img src="${url}" alt="${alt}"></figure>`;
}

function createVideoElement(url, type) {
    return `<figure class="media"><video controls><source src="${url}" type="${type}"></video></figure>`;
}

// Create preview elements for different file types
async function createAttachmentPreview(file) {
    const div = document.createElement('div');
    div.className = 'attachment-preview';
    div.setAttribute('draggable', true);
    div.setAttribute('data-url', file.url);
    div.setAttribute('data-type', file.type);
    
    const fileItem = document.createElement('div');
    fileItem.className = 'file-item';
    
    const icon = document.createElement('i');
    icon.className = 'bi ' + fileTypeUtils.getFileIcon(file);
    fileItem.appendChild(icon);
    
    const nameSpan = document.createElement('span');
    nameSpan.className = 'file-item-name';
    nameSpan.textContent = file.name;
    fileItem.appendChild(nameSpan);
    
    const sizeSpan = document.createElement('span');
    sizeSpan.className = 'file-item-size';
    sizeSpan.textContent = fileTypeUtils.formatFileSize(file.size);
    fileItem.appendChild(sizeSpan);
    
    div.appendChild(fileItem);
    
    if (fileTypeUtils.isImage(file)) {
        const img = document.createElement('img');
        img.src = file.url;
        img.alt = file.name;
        img.loading = 'lazy';
        div.appendChild(img);
    } else if (fileTypeUtils.isVideo(file)) {
        const video = document.createElement('video');
        video.src = file.url;
        video.controls = true;
        div.appendChild(video);
    }
    
    return div;
}

// File handling utilities
const FileUtils = {
    // Get human readable file size
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    // Get appropriate icon for file type
    getFileIcon(mimeType) {
        const icons = {
            'image': 'fa-image',
            'video': 'fa-video',
            'audio': 'fa-music',
            'text': 'fa-file-alt',
            'application/pdf': 'fa-file-pdf',
            'application/msword': 'fa-file-word',
            'application/vnd.ms-excel': 'fa-file-excel',
            'application/vnd.ms-powerpoint': 'fa-file-powerpoint'
        };

        const type = mimeType.split('/')[0];
        return icons[mimeType] || icons[type] || 'fa-file';
    },

    // Initialize drag and drop handlers
    initDragAndDrop(dropZone, onFilesDrop) {
        const preventDefault = (e) => {
            e.preventDefault();
            e.stopPropagation();
        };

        dropZone.addEventListener('dragenter', (e) => {
            preventDefault(e);
            dropZone.classList.add('drag-over');
        });

        dropZone.addEventListener('dragover', preventDefault);

        dropZone.addEventListener('dragleave', (e) => {
            preventDefault(e);
            dropZone.classList.remove('drag-over');
        });

        dropZone.addEventListener('drop', (e) => {
            preventDefault(e);
            dropZone.classList.remove('drag-over');
            const files = Array.from(e.dataTransfer.files);
            onFilesDrop(files);
        });

        // Handle click to upload
        dropZone.addEventListener('click', () => {
            const input = document.createElement('input');
            input.type = 'file';
            input.multiple = true;
            input.onchange = (e) => {
                const files = Array.from(e.target.files);
                onFilesDrop(files);
            };
            input.click();
        });
    },

    // Create preview element for file
    createPreviewElement(file) {
        const preview = document.createElement('div');
        preview.className = 'attachment-preview';
        preview.setAttribute('draggable', true);

        const fileItem = document.createElement('div');
        fileItem.className = 'file-item';

        const icon = document.createElement('i');
        icon.className = `fas ${this.getFileIcon(file.type)}`;
        fileItem.appendChild(icon);

        const nameSpan = document.createElement('span');
        nameSpan.className = 'file-item-name';
        nameSpan.textContent = file.name;
        fileItem.appendChild(nameSpan);

        const sizeSpan = document.createElement('span');
        sizeSpan.className = 'file-item-size';
        sizeSpan.textContent = this.formatFileSize(file.size);
        fileItem.appendChild(sizeSpan);

        preview.appendChild(fileItem);

        // Add preview for images and videos
        if (file.type.startsWith('image/')) {
            const img = document.createElement('img');
            img.src = URL.createObjectURL(file);
            preview.appendChild(img);
        } else if (file.type.startsWith('video/')) {
            const video = document.createElement('video');
            video.src = URL.createObjectURL(file);
            video.controls = true;
            preview.appendChild(video);
        }

        return preview;
    },

    // Handle file upload with progress
    async uploadFile(file, url, onProgress) {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch(url, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error uploading file:', error);
            throw error;
        }
    }
};

// Export the utilities
window.FileUtils = FileUtils;