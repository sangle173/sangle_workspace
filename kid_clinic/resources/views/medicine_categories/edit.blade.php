@extends('layouts.app')

@section('content')
    <h1>Edit Medicine Category</h1>
    <form action="{{ route('medicine-categories.update', $medicineCategory) }}" method="POST">
        @csrf
        @method('PUT')
        <div class="mb-3">
            <label for="name" class="form-label">Category Name</label>
            <input type="text" id="name" name="name" class="form-control" value="{{ $medicineCategory->name }}" required>
        </div>
        <button type="submit" class="btn btn-success">Save</button>
        <a href="{{ route('medicine-categories.index') }}" class="btn btn-secondary">Back</a>
    </form>
@endsection
