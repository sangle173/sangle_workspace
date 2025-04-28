@extends('layouts.app')
@section('content')
<form action="{{ $action }}" method="POST" enctype="multipart/form-data">
    @csrf
    @if($method === 'PUT')
        @method('PUT')
    @endif

    <div class="mb-3">
        <label class="form-label">Notebook</label>
        <select name="notebook_id" class="form-control" required>
    <option value="">Select Notebook</option>
    @foreach($notebooks as $notebook)
        <option value="{{ $notebook->id }}"
            {{ (old('notebook_id', $preselectedNotebookId ?? $note->notebook_id ?? '') == $notebook->id) ? 'selected' : '' }}>
            {{ $notebook->name }}
        </option>
    @endforeach
</select>




    </div>

    <div class="mb-3">
        <label class="form-label">Note Title</label>
        <input type="text" name="title" class="form-control"
               value="{{ old('title', $note->title ?? '') }}" required>
    </div>

    <div class="mb-3">
        <label class="form-label">Content</label>
        <textarea name="content" class="form-control tiny-editor" rows="10">{{ old('body', $note->body ?? '') }}</textarea>
    </div>

    <button type="submit" class="btn btn-primary">Save</button>
</form>

@include('notes.partials._tinymce_script') <!-- ✅ Include TinyMCE -->
@endsection
