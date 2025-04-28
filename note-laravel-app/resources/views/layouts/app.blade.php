<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Notes App</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body, html {
            height: 100%;
            overflow: hidden;
        }
        #wrapper {
            display: flex;
            height: 100%;
        }
        #sidebar {
            width: 250px;
            background: #f8f9fa;
            transition: width 0.3s, padding 0.3s;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            padding: 1rem;
            position: relative;
        }
        #sidebar.collapsed {
            width: 60px;
            padding: 1rem 0.5rem;
            overflow-x: hidden;
        }
        #content {
            flex: 1;
            overflow-y: auto;
            padding: 1.5rem;
            transition: margin-left 0.3s;
        }
        .notebook-header:hover .create-note-btn {
            display: inline-block !important;
        }
        .notebook-item {
            position: relative;
        }
        .notebook-item:hover .new-note-btn {
            display: inline-block !important;
        }
        .new-note-btn {
            display: none;
        }
        .sidebar-footer {
            margin-top: auto;
            text-align: center;
            padding-top: 1rem;
        }
        /* Always show toggle button even if collapsed */
        #toggleSidebar {
            width: 100%;
        }

        a.active-note {
    background-color: #d1e7dd;
    border-radius: 5px;
    padding: 4px 8px;
    color: #0f5132 !important;
    font-weight: bold;
}
    </style>
</head>

<body>
<div id="wrapper">
    <div id="sidebar" class="border-end">

        <!-- Notebooks + Notes Tree -->
        @include('partials.sidebar')

        <!-- Sidebar Footer (Toggle Button Always visible) -->
        <div class="sidebar-footer">
            <button id="toggleSidebar" class="btn btn-outline-secondary btn-sm">☰ Toggle Sidebar</button>
        </div>
    </div>

    <div id="content">
        @yield('content')
    </div>
</div>

<script>
    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('toggleSidebar');
    const sidebarTitle = document.getElementById('sidebarTitle');
    const newNotebookBtn = document.getElementById('newNotebookBtn');
    const searchContainer = document.getElementById('searchContainer');

    toggleBtn.addEventListener('click', function () {
        sidebar.classList.toggle('collapsed');

        if (sidebar.classList.contains('collapsed')) {
            sidebarTitle.textContent = "📒";  // Shorten title
            newNotebookBtn.classList.add('d-none'); // Hide "+New"
            searchContainer.classList.add('d-none'); // Hide search
        } else {
            sidebarTitle.textContent = "📒 Notebooks";
            newNotebookBtn.classList.remove('d-none');
            searchContainer.classList.remove('d-none');
        }
    });

    // Toggle expand/collapse notes inside notebooks
    document.querySelectorAll('.toggle-notes').forEach(btn => {
        btn.addEventListener('click', function() {
            const notesList = this.closest('.notebook-item').querySelector('.notes-list');
            notesList.classList.toggle('d-none');
            this.textContent = notesList.classList.contains('d-none') ? '▶' : '▼';
        });
    });

    // Show create-note button on hover
    document.querySelectorAll('.notebook-item').forEach(item => {
        item.addEventListener('mouseenter', function() {
            const createBtn = this.querySelector('.create-note-btn');
            if (createBtn) createBtn.classList.remove('d-none');
        });
        item.addEventListener('mouseleave', function() {
            const createBtn = this.querySelector('.create-note-btn');
            if (createBtn) createBtn.classList.add('d-none');
        });
    });

    // Live search
    document.getElementById('searchInput').addEventListener('input', function() {
        const search = this.value.toLowerCase();
        document.querySelectorAll('.note-item').forEach(function(item) {
            const text = item.innerText.toLowerCase();
            item.style.display = text.includes(search) ? 'block' : 'none';
        });
    });
</script>

@stack('scripts')
</body>
</html>
