<!-- Sidebar Header -->
<div class="d-flex justify-content-between align-items-center mb-2">
    <h5 class="mb-0">📒 Notebooks</h5>
    <a href="{{ route('notebooks.create') }}" class="btn btn-primary btn-sm ms-2">+ New</a>
</div>

<!-- Search box -->
<div class="mb-3">
    <input type="text" id="searchInput" class="form-control form-control-sm" placeholder="🔍 Search notes...">
</div>

<!-- Notebooks List -->
<div id="notebooksList">
    @foreach($notebooks as $notebook)
    <div class="mb-2 notebook-item position-relative">
        <div class="d-flex align-items-center justify-content-between notebook-header">
            <div class="d-flex align-items-center">
                <button class="btn btn-sm btn-link text-decoration-none p-0 me-1 toggle-notes" style="font-size: 14px;">
                    ▼
                </button>
                <a href="{{ route('notebooks.edit', $notebook) }}" class="fw-bold text-dark text-decoration-none">
                    📁 {{ $notebook->name }}
                </a>
            </div>

            <a href="{{ route('notes.create', ['notebook_id' => $notebook->id]) }}" 
               class="btn btn-success btn-sm ms-2 d-none create-note-btn">+
            </a>
        </div>

        <div class="ms-4 notes-list">
        @foreach($notebook->notes as $noteItem)
    <div class="note-item">
        <a href="{{ route('notes.edit', $noteItem) }}" 
           class="d-block small text-dark text-decoration-none
                  {{ (isset($note) && $note->id === $noteItem->id) ? 'active-note' : '' }}">
            📝 {{ $noteItem->title }}
        </a>
    </div>
@endforeach

        </div>
    </div>
    @endforeach
</div>


@push('scripts')
<script>
// Toggle collapse notebooks
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
        this.querySelector('.create-note-btn').classList.remove('d-none');
    });
    item.addEventListener('mouseleave', function() {
        this.querySelector('.create-note-btn').classList.add('d-none');
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
@endpush
