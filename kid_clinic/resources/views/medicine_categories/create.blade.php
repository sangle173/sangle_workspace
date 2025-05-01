@extends('layouts.app')

@section('content')
    <h1>Add New Medicine Category</h1>
    <form action="{{ route('medicine-categories.store') }}" method="POST">
        @csrf
        <div class="mb-3">
            <label for="name" class="form-label">Category Name</label>
            <input type="text" id="name" name="name" class="form-control" required>
        </div>
        <button type="submit" class="btn btn-success">Save</button>
        <a href="{{ route('medicine-categories.index') }}" class="btn btn-secondary">Back</a>
    </form>
@endsection
