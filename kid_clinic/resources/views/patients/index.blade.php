@extends('layouts.app')

@section('content')
    <div class="card shadow-sm mb-4">
        <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h1 class="h4 mb-0"><i class="fa-solid fa-user-injured me-2"></i> {{ __('messages.patients') }}</h1>
                <a href="{{ route('patients.create') }}" class="btn btn-primary">
                    <i class="fa-solid fa-plus me-1"></i> {{ __('messages.add_new') }}
                </a>
            </div>
            <!-- Search & Filter Form -->
            <form action="{{ route('patients.index') }}" method="GET" class="mb-4">
                <div class="row g-3 align-items-end">
                    <div class="col-md-5">
                        <label for="search" class="form-label">{{ __('messages.search') }}</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-user"></i></span>
                            <input type="text" id="search" name="search" class="form-control" placeholder="{{ __('messages.name') }}" value="{{ $search }}">
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label for="address_id" class="form-label">{{ __('messages.address') }}</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-location-dot"></i></span>
                            <select id="address_id" name="address_id" class="form-select">
                                <option value="">{{ __('messages.all_addresses') }}</option>
                                @foreach($addresses as $address)
                                    <option value="{{ $address->id }}" {{ $selectedAddress == $address->id ? 'selected' : '' }}>{{ $address->name }}</option>
                                @endforeach
                            </select>
                        </div>
                    </div>
                    <div class="col-md-2 d-flex gap-2">
                        <button type="submit" class="btn btn-primary flex-grow-1" title="{{ __('messages.filter') }}" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.filter') }}">
                            <i class="fa-solid fa-filter"></i>
                        </button>
                        <a href="{{ route('patients.index') }}" class="btn btn-secondary flex-grow-1" title="{{ __('messages.reset') }}" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.reset') }}">
                            <i class="fa-solid fa-rotate-right"></i>
                        </a>
                    </div>
                </div>
            </form>
            <div class="table-responsive">
                <table class="table table-striped table-hover align-middle">
                    <thead class="table-light">
                    <tr>
                        <th>#</th>
                        <th>{{ __('messages.name') }}</th>
                        <th>{{ __('messages.gender') }}</th>
                        <th>{{ __('messages.address') }}</th>
                        <th>{{ __('messages.phone') }}</th>
                        <th>{{ __('messages.weight') }}</th>
                        <th>{{ __('messages.height') }}</th>
                        <th>{{ __('messages.updated_at') }}</th>
                        <th>{{ __('messages.actions') }}</th>
                    </tr>
                    </thead>
                    <tbody>
                    @forelse($patients as $patient)
                        <tr>
                            <td>{{ $loop->iteration }}</td>
                            <td>{{ $patient->name }}</td>
                            <td>
                                <span class="badge {{ $patient->gender == 'male' ? 'bg-primary' : 'bg-pink' }}">{{ $patient->gender == 'male' ? __('messages.male') : __('messages.female') }}</span>
                            </td>
                            <td>{{ $patient->address->name }}</td>
                            <td>{{ $patient->phone_number }}</td>
                            <td>{{ $patient->weight }} kg</td>
                            <td>{{ $patient->height }} cm</td>
                            <td>{{ $patient->updated_at ? $patient->updated_at->format('d/m/Y H:i') : $patient->created_at->format('d/m/Y H:i') }}</td>
                            <td class="d-flex gap-1">
                                <a href="{{ route('patients.examine-histories', $patient->id) }}" class="btn btn-info btn-sm" title="{{ __('messages.examine_histories') }}" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.examine_histories') }}">
                                    <i class="fa-solid fa-notes-medical"></i>
                                </a>
                                <a href="{{ route('patients.edit', $patient->id) }}" class="btn btn-warning btn-sm" title="{{ __('messages.edit') }}" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.edit') }}">
                                    <i class="fa-solid fa-pen-to-square"></i>
                                </a>
                                <form action="{{ route('patients.destroy', $patient->id) }}" method="POST" class="d-inline">
                                    @csrf
                                    @method('DELETE')
                                    <button class="btn btn-danger btn-sm" onclick="return confirm('{{ __('messages.delete') }}?')" title="{{ __('messages.delete') }}" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.delete') }}">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </form>
                            </td>
                        </tr>
                    @empty
                        <tr>
                            <td colspan="9" class="text-center text-muted py-5">
                                <i class="fa-solid fa-folder-open fa-2x mb-2"></i><br>
                                <span>{{ __('messages.no_results') }}</span>
                            </td>
                        </tr>
                    @endforelse
                    </tbody>
                </table>
            </div>
            <div class="mt-3">
                {{ $patients->links() }}
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
