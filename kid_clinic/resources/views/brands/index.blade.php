@extends('layouts.app')

@section('content')
    <div class="card shadow-sm mb-4">
        <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h1 class="h4 mb-0"><i class="fa-solid fa-copyright me-2"></i> {{ __('messages.brands') }}</h1>
                <a href="{{ route('brands.create') }}" class="btn btn-primary">
                    <i class="fa-solid fa-plus me-1"></i> {{ __('messages.add_new') }}
                </a>
            </div>
            <div class="table-responsive">
                <table class="table table-striped table-hover align-middle">
                    <thead class="table-light">
                    <tr>
                        <th>#</th>
                        <th>{{ __('messages.name') }}</th>
                        <th>{{ __('messages.status') }}</th>
                        <th>{{ __('messages.actions') }}</th>
                    </tr>
                    </thead>
                    <tbody>
                    @forelse($brands as $index => $brand)
                        <tr>
                            <td>{{ $loop->iteration + ($brands->currentPage() - 1) * $brands->perPage() }}</td>
                            <td>{{ $brand->name }}</td>
                            <td>
                                <span class="badge {{ $brand->status ? 'bg-success' : 'bg-secondary' }}">{{ $brand->status ? 'Active' : 'Inactive' }}</span>
                            </td>
                            <td class="d-flex gap-1">
                                <a href="{{ route('brands.edit', $brand->id) }}" class="btn btn-warning btn-sm" title="Edit" data-bs-toggle="tooltip" data-bs-title="Edit">
                                    <i class="fa-solid fa-pen-to-square"></i>
                                </a>
                                <form action="{{ route('brands.destroy', $brand->id) }}" method="POST" class="d-inline">
                                    @csrf
                                    @method('DELETE')
                                    <button class="btn btn-danger btn-sm" onclick="return confirm('Are you sure?')" title="Delete" data-bs-toggle="tooltip" data-bs-title="Delete">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </form>
                            </td>
                        </tr>
                    @empty
                        <tr>
                            <td colspan="4" class="text-center text-muted py-5">
                                <i class="fa-solid fa-folder-open fa-2x mb-2"></i><br>
                                <span>{{ __('messages.no_results') }}</span>
                            </td>
                        </tr>
                    @endforelse
                    </tbody>
                </table>
            </div>
            <div class="mt-3">
                {{ $brands->links() }}
            </div>
        </div>
    </div>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
        });
    </script>
@endsection
