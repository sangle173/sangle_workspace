@extends('layouts.app')

@section('content')
    <div class="card shadow-sm mb-4">
        <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h1 class="h4 mb-0"><i class="fa-solid fa-capsules me-2"></i> {{ __('messages.medicines') }}</h1>
                <a href="{{ route('medicines.create') }}" class="btn btn-primary">
                    <i class="fa-solid fa-plus me-1"></i> {{ __('messages.add_new') }}
                </a>
            </div>
            <!-- Search and Filter Form -->
            <form action="{{ route('medicines.index') }}" method="GET" class="mb-4">
                <div class="row g-3 align-items-end">
                    <div class="col-md-4">
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-search"></i></span>
                            <input type="text" name="search" class="form-control" placeholder="{{ __('messages.search') }}" value="{{ $search }}">
                        </div>
                    </div>
                    <div class="col-md-2">
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-layer-group"></i></span>
                            <select name="category_id" class="form-select">
                                <option value="">{{ __('messages.category') }}</option>
                                @foreach($categories as $category)
                                    <option value="{{ $category->id }}" {{ $categoryId == $category->id ? 'selected' : '' }}>{{ $category->name }}</option>
                                @endforeach
                            </select>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-clipboard-check"></i></span>
                            <select name="medicine_status_id" class="form-select">
                                <option value="">{{ __('messages.medicine_status') }}</option>
                                @foreach($statuses as $status)
                                    <option value="{{ $status->id }}" {{ $statusId == $status->id ? 'selected' : '' }}>{{ $status->name }}</option>
                                @endforeach
                            </select>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-copyright"></i></span>
                            <select name="brand_id" class="form-select">
                                <option value="">{{ __('messages.brand') }}</option>
                                @foreach($brands as $brand)
                                    <option value="{{ $brand->id }}" {{ $brandId == $brand->id ? 'selected' : '' }}>{{ $brand->name }}</option>
                                @endforeach
                            </select>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-ruler-combined"></i></span>
                            <select name="unit_id" class="form-select">
                                <option value="">{{ __('messages.unit') }}</option>
                                @foreach($units as $unit)
                                    <option value="{{ $unit->id }}" {{ $unitId == $unit->id ? 'selected' : '' }}>{{ $unit->name }}</option>
                                @endforeach
                            </select>
                        </div>
                    </div>
                    <div class="col-md-2 d-flex gap-2">
                        <button type="submit" class="btn btn-primary flex-grow-1" title="{{ __('messages.filter') }}" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.filter') }}">
                            <i class="fa-solid fa-filter"></i>
                        </button>
                        <a href="{{ route('medicines.index') }}" class="btn btn-secondary flex-grow-1" title="{{ __('messages.reset') }}" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.reset') }}">
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
                        <th>{{ __('messages.image') }}</th>
                        <th>{{ __('messages.name') }}</th>
                        <th>{{ __('messages.category') }}</th>
                        <th>{{ __('messages.unit') }}</th>
                        <th>{{ __('messages.brand') }}</th>
                        <th>{{ __('messages.medicine_status') }}</th>
                        <th>{{ __('messages.quantity') }}</th>
                        <th>{{ __('messages.price') }}</th>
                        <th>{{ __('messages.manufacture_date') }}</th>
                        <th>{{ __('messages.expired_date') }}</th>
                        <th>{{ __('messages.created_at') }}</th>
                        <th>{{ __('messages.actions') }}</th>
                    </tr>
                    </thead>
                    <tbody>
                    @forelse($medicines as $medicine)
                        <tr>
                            <td>{{ $loop->iteration }}</td>
                            <td>
                                <img src="{{ $medicine->image ? asset('storage/' . $medicine->image) : asset('images/no-image.png') }}"
                                     alt="{{ $medicine->name }}"
                                     class="img-thumbnail border-0 shadow-sm"
                                     style="max-width: 80px; height: auto; cursor: pointer;"
                                     data-bs-toggle="modal"
                                     data-bs-target="#imageModal"
                                     data-bs-image="{{ $medicine->image ? asset('storage/' . $medicine->image) : asset('images/no-image.png') }}">
                            </td>
                            <td>{{ $medicine->name }}</td>
                            <td><span class="badge bg-info text-dark">{{ $medicine->category->name }}</span></td>
                            <td><span class="badge bg-secondary">{{ $medicine->unit->name }}</span></td>
                            <td><span class="badge bg-warning text-dark">{{ $medicine->brand->name }}</span></td>
                            <td><span class="badge {{ $medicine->medicineStatus->name == 'Active' ? 'bg-success' : 'bg-secondary' }}">{{ $medicine->medicineStatus->name }}</span></td>
                            <td><span class="badge bg-primary">{{ $medicine->quantity }}</span></td>
                            <td><span class="badge bg-success">{{ number_format($medicine->price / 100, 2) }}</span></td>
                            <td>{{ $medicine->manufacture_date ? $medicine->manufacture_date->format('d/m/Y') : __('messages.no_data') }}</td>
                            <td>{{ $medicine->expired_date ? $medicine->expired_date->format('d/m/Y') : __('messages.no_data') }}</td>
                            <td>{{ $medicine->created_at }}</td>
                            <td class="d-flex gap-1">
                                <a href="{{ route('medicines.edit', $medicine->id) }}" class="btn btn-warning btn-sm" title="{{ __('messages.edit') }}" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.edit') }}">
                                    <i class="fa-solid fa-pen-to-square"></i>
                                </a>
                                <form action="{{ route('medicines.destroy', $medicine->id) }}" method="POST" class="d-inline">
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
                            <td colspan="13" class="text-center text-muted py-5">
                                <i class="fa-solid fa-folder-open fa-2x mb-2"></i><br>
                                <span>{{ __('messages.no_results') }}</span>
                            </td>
                        </tr>
                    @endforelse
                    </tbody>
                </table>
            </div>
            <div class="mt-3">
                {{ $medicines->links() }}
            </div>
        </div>
    </div>
    <!-- Modal -->
    <div class="modal fade" id="imageModal" tabindex="-1" aria-labelledby="imageModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="imageModalLabel">{{ __('messages.image_preview') }}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body text-center">
                    <img id="modalImage" src="" alt="{{ __('messages.image') }}" class="img-fluid rounded shadow">
                </div>
            </div>
        </div>
    </div>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
            // Modal image preview
            var imageModal = document.getElementById('imageModal');
            var modalImage = document.getElementById('modalImage');
            imageModal.addEventListener('show.bs.modal', function (event) {
                var button = event.relatedTarget;
                var imageSrc = button.getAttribute('data-bs-image');
                modalImage.setAttribute('src', imageSrc);
            });
        });
    </script>
@endsection
