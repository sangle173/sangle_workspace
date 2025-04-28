@extends('layouts.app')

@section('content')
<div class="container-fluid">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="mb-0">Notebooks</h2>
        <a href="{{ route('notebooks.create') }}" class="btn btn-primary">+ New Notebook</a>
    </div>

    @if (session('success'))
        <div class="alert alert-success">{{ session('success') }}</div>
    @endif

    <div class="list-group">
        @forelse($notebooks as $notebook)
            <div class="list-group-item d-flex justify-content-between align-items-center">
                <a href="{{ route('notebooks.show', $notebook) }}" class="fw-bold">{{ $notebook->name }}</a> {{-- 🔥 name, not title --}}
                <div>
                    <a href="{{ route('notebooks.edit', $notebook) }}" class="btn btn-sm btn-outline-warning me-2">Edit</a>

                    <form action="{{ route('notebooks.destroy', $notebook) }}" method="POST" class="d-inline">
                        @csrf
                        @method('DELETE')
                        <button onclick="return confirm('Are you sure you want to delete this notebook?')" 
                                class="btn btn-sm btn-outline-danger">
                            Delete
                        </button>
                    </form>
                </div>
            </div>
        @empty
            <div class="alert alert-info">
                No notebooks found. Create your first notebook!
            </div>
        @endforelse
    </div>
</div>
@endsection
