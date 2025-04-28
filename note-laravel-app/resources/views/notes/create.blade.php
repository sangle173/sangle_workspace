@extends('layouts.app')

@section('content')
<div class="container-fluid">
    <h2>Create Note</h2>

    @include('notes._form', [
        'action' => route('notes.store'),
        'method' => 'POST',
        'note' => null,
        'notebooks' => $notebooks
    ])
</div>
@endsection

@section('scripts')
@include('notes.partials._tinymce_script')
@endsection
