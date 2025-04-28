@extends('layouts.app')

@section('content')
    <h2>{{ $notebook->name }}</h2>

    <a href="{{ route('notebooks.edit', $notebook) }}" class="btn btn-warning">Edit Notebook</a>

    <form action="{{ route('notebooks.destroy', $notebook) }}" method="POST" style="display:inline-block;">
        @csrf
        @method('DELETE')
        <button type="submit" class="btn btn-danger">Delete Notebook</button>
    </form>

    <hr>

    <p>Notes inside this notebook (coming soon...)</p>
@endsection
