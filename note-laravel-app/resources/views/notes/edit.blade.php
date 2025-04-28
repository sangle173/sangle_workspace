@extends('layouts.app')

@section('content')  <!-- ✅ START FIRST -->

<div class="container-fluid">

    @isset($note)
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div class="d-flex align-items-center">
            <span class="me-2 fw-bold">{{ $note->notebook->name }}</span>
            <span class="me-2">›</span>

            <form id="inlineTitleForm" action="{{ route('notes.updateTitle', $note) }}" method="POST" class="d-inline">
                @csrf
                @method('PUT')
                <input type="text" name="title" value="{{ $note->title }}" 
                    class="form-control form-control-sm d-inline-block" 
                    style="width: 300px;"
                    onchange="document.getElementById('inlineTitleForm').submit();" />
            </form>
        </div>

        <div class="text-muted small">
            Last updated: {{ $note->updated_at->format('d/m/Y H:i') }}
        </div>
    </div>
    @endisset

    @include('notes._form', [
        'action' => route('notes.update', $note),
        'method' => 'PUT',
        'note' => $note,
        'notebooks' => $notebooks
    ])

</div>

@endsection   <!-- ✅ MUST END -->
