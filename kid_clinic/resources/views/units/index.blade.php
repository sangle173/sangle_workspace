@extends('layouts.app')

@section('content')
    <h1>Units</h1>
    <a href="{{ route('units.create') }}" class="btn btn-primary mb-3">Add New Unit</a>

    <table class="table table-bordered">
        <thead>
        <tr>
            <th>#</th>
            <th>Name</th>
            <th>Status</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        @foreach($units as $index => $unit)
            <tr>
                <td>{{ $loop->iteration + ($units->currentPage() - 1) * $units->perPage() }}</td>
                <td>{{ $unit->name }}</td>
                <td>{{ $unit->status ? 'Active' : 'Inactive' }}</td>
                <td>
                    <a href="{{ route('units.edit', $unit->id) }}" class="btn btn-warning btn-sm">Edit</a>
                    <form action="{{ route('units.destroy', $unit->id) }}" method="POST" class="d-inline">
                        @csrf
                        @method('DELETE')
                        <button class="btn btn-danger btn-sm" onclick="return confirm('Are you sure?')">Delete</button>
                    </form>
                </td>
            </tr>
        @endforeach
        </tbody>
    </table>
    {{ $units->links() }}
@endsection
