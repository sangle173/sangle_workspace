@extends('layouts.app')

@section('content')
    <h1>Medicine Statuses</h1>
    <a href="{{ route('medicine-statuses.create') }}" class="btn btn-primary mb-3">Add New Medicine Status</a>

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
        @foreach($medicineStatuses as $index => $status)
            <tr>
                <td>{{ $loop->iteration + ($medicineStatuses->currentPage() - 1) * $medicineStatuses->perPage() }}</td>
                <td>{{ $status->name }}</td>
                <td>{{ $status->status ? 'Active' : 'Inactive' }}</td>
                <td>
                    <a href="{{ route('medicine-statuses.edit', $status->id) }}" class="btn btn-warning btn-sm">Edit</a>
                    <form action="{{ route('medicine-statuses.destroy', $status->id) }}" method="POST" class="d-inline">
                        @csrf
                        @method('DELETE')
                        <button class="btn btn-danger btn-sm" onclick="return confirm('Are you sure?')">Delete</button>
                    </form>
                </td>
            </tr>
        @endforeach
        </tbody>
    </table>
    {{ $medicineStatuses->links() }}
@endsection
