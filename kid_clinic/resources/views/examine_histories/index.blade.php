@extends('layouts.app')

@section('content')
    <div class="card shadow-sm mb-4">
        <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-3">
                <h1 class="h4 mb-0"><i class="fa-solid fa-notes-medical me-2"></i> {{ __('messages.examine_histories') }}</h1>
                <a href="{{ route('examine-histories.create') }}" class="btn btn-primary">
                    <i class="fa-solid fa-plus me-1"></i> {{ __('messages.add_new') }}
                </a>
            </div>
            <div class="mb-4">
                <form method="GET" class="row g-2 align-items-end">
                    <div class="col-auto">
                        <label for="start_date" class="form-label mb-0">Từ ngày</label>
                        <input type="date" id="start_date" name="start_date" class="form-control" value="{{ $startDate }}">
                    </div>
                    <div class="col-auto">
                        <label for="end_date" class="form-label mb-0">Đến ngày</label>
                        <input type="date" id="end_date" name="end_date" class="form-control" value="{{ $endDate }}">
                    </div>
                    <div class="col-auto">
                        <a href="?start_date={{ $today }}&end_date={{ $today }}" class="btn {{ ($startDate == $today && $endDate == $today) ? 'btn-primary' : 'btn-outline-secondary' }}">Hôm nay</a>
                        <a href="?start_date={{ $weekStart }}&end_date={{ $weekEnd }}" class="btn {{ ($startDate == $weekStart && $endDate == $weekEnd) ? 'btn-primary' : 'btn-outline-secondary' }}">Tuần này</a>
                        <a href="?start_date={{ $monthStart }}&end_date={{ $monthEnd }}" class="btn {{ ($startDate == $monthStart && $endDate == $monthEnd) ? 'btn-primary' : 'btn-outline-secondary' }}">Tháng này</a>
                    </div>
                </form>
            </div>
            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <div class="card text-white bg-primary h-100">
                        <div class="card-body">
                            <h6 class="card-title">Số lượt khám</h6>
                            <h3 class="card-text">{{ number_format($summary['total']) }}</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card text-white bg-success h-100">
                        <div class="card-body">
                            <h6 class="card-title">Tổng phí</h6>
                            <h3 class="card-text">{{ number_format($summary['total_fee'], 0, ',', '.') }} ₫</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card text-white bg-warning h-100">
                        <div class="card-body">
                            <h6 class="card-title">Tổng lợi nhuận</h6>
                            <h3 class="card-text">{{ number_format($summary['total_profit'], 0, ',', '.') }} ₫</h3>
                        </div>
                    </div>
                </div>
            </div>

           
            <!-- Search & Filter Form -->
            <form action="{{ route('examine-histories.index') }}" method="GET" class="mb-4">
                <div class="row g-3 align-items-end">
                    <div class="col-md-4">
                        <label for="search" class="form-label">{{ __('messages.search') }}</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-user"></i></span>
                            <input type="text" id="search" name="search" class="form-control" placeholder="{{ __('messages.patient_name') }}" value="{{ $search }}">
                        </div>
                    </div>
                    <div class="col-md-2 d-flex gap-2">
                        <button type="submit" class="btn btn-primary flex-grow-1" title="{{ __('messages.filter') }}" data-bs-toggle="tooltip" data-bs-placement="top" data-bs-title="{{ __('messages.filter') }}">
                            <i class="fa-solid fa-filter"></i>
                        </button>
                        <a href="{{ route('examine-histories.index') }}" class="btn btn-secondary flex-grow-1" title="{{ __('messages.reset') }}" data-bs-toggle="tooltip" data-bs-placement="top" data-bs-title="{{ __('messages.reset') }}">
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
                        <th>{{ __('messages.patient_name') }}</th>
                        <th>{{ __('messages.diagnose') }}</th>
                        <th>{{ __('messages.symptoms') }}</th>
                        <th>{{ __('messages.prescription') }}</th>
                        <th>{{ __('messages.fee') }}</th>
                        <th>Lợi nhuận</th>
                        <th>{{ __('messages.updated_at') }}</th>
                        <th>{{ __('messages.actions') }}</th>
                    </tr>
                    </thead>
                    <tbody>
                    @forelse($examineHistories as $history)
                        <tr>
                            <td>{{ $loop->iteration }}</td>
                            <td>
                                <a href="javascript:void(0);" 
                                   class="fw-bold text-primary patient-detail-link" 
                                   data-patient='@json($history->patient)'
                                   data-address="{{ $history->patient->address->name ?? '' }}">
                                    {{ $history->patient->name }}
                                </a>
                            </td>
                            <td>
                                <ul class="mb-0 ps-3">
                                    @foreach(explode(',', $history->diagnose) as $diagnose)
                                        <li>{{ trim($diagnose) }}</li>
                                    @endforeach
                                </ul>
                            </td>
                            <td>
                                <ul class="mb-0 ps-3">
                                    @foreach(explode(',', $history->symptoms) as $symptom)
                                        <li>{{ trim($symptom) }}</li>
                                    @endforeach
                                </ul>
                            </td>
                            <td>
                                @php
                                    $prescriptions = json_decode($history->prescription, true);
                                @endphp
                                @if(!empty($prescriptions))
                                    <ul class="mb-0 ps-3">
                                        @foreach($prescriptions as $prescription)
                                            @php
                                                $medicine = \App\Models\Medicine::find($prescription['medicine_id']);
                                            @endphp
                                            <li>
                                                <span class="fw-semibold text-dark">{{ $medicine ? $medicine->name : __('messages.unknown_medicine') }}</span>
                                                <span class="badge bg-primary ms-2">{{ $prescription['quantity'] }}</span>
                                            </li>
                                        @endforeach
                                    </ul>
                                @else
                                    <span class="text-muted small"><i class="fa-regular fa-circle-xmark me-1"></i>{{ __('messages.no_medicine_prescribed') }}</span>
                                @endif
                            </td>
                            <td>
                                {{ $history->fee ? number_format($history->fee, 0, ',', '.') . ' ₫' : '-' }}
                            </td>
                            <td>
                                {{ $history->profit ? number_format($history->profit, 0, ',', '.') . ' ₫' : '-' }}
                            </td>
                            <td>
                                {{ $history->updated_at ? $history->updated_at->format('d/m/Y H:i') : '' }}
                            </td>
                            <td>
                                <a href="{{ route('examine-histories.edit', $history->id) }}" class="btn btn-warning btn-sm me-1" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.edit') }}">
                                    <i class="fa-solid fa-pen-to-square"></i>
                                </a>
                                <form action="{{ route('examine-histories.destroy', $history->id) }}" method="POST" class="d-inline">
                                    @csrf
                                    @method('DELETE')
                                    <button class="btn btn-danger btn-sm" onclick="return confirm('{{ __('messages.delete') }}?')" data-bs-toggle="tooltip" data-bs-title="{{ __('messages.delete') }}">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </form>
                            </td>
                        </tr>
                    @empty
                        <tr>
                            <td colspan="6" class="text-center text-muted py-5">
                                <i class="fa-solid fa-folder-open fa-2x mb-2"></i><br>
                                <span>{{ __('messages.no_results') }}</span>
                            </td>
                        </tr>
                    @endforelse
                    </tbody>
                </table>
            </div>
            <!-- Pagination -->
            <div class="mt-3">
                {{ $examineHistories->links() }}
            </div>
        </div>
    </div>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });

            document.querySelectorAll('.patient-detail-link').forEach(function(link) {
                link.addEventListener('click', function() {
                    var patient = JSON.parse(this.getAttribute('data-patient'));
                    document.getElementById('modal-patient-name').textContent = patient.name || '';
                    document.getElementById('modal-patient-gender').textContent = patient.gender === 'male' ? 'Nam' : 'Nữ';
                    document.getElementById('modal-patient-address').textContent = this.getAttribute('data-address') || '';
                    document.getElementById('modal-patient-dob').textContent = patient.date_of_birth || '';
                    document.getElementById('modal-patient-weight').textContent = patient.weight || '';
                    document.getElementById('modal-patient-height').textContent = patient.height || '';
                    document.getElementById('modal-patient-phone').textContent = patient.phone_number || '';
                    document.getElementById('modal-patient-note').textContent = patient.note || '';
                    var modal = new bootstrap.Modal(document.getElementById('patientDetailModal'));
                    modal.show();
                });
            });
        });
    </script>
    <!-- Patient Detail Modal -->
    <div class="modal fade" id="patientDetailModal" tabindex="-1" aria-labelledby="patientDetailModalLabel" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="patientDetailModalLabel">Thông tin bệnh nhân</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <ul class="list-group">
              <li class="list-group-item"><strong>{{ __('messages.name') }}:</strong> <span id="modal-patient-name"></span></li>
              <li class="list-group-item"><strong>{{ __('messages.gender') }}:</strong> <span id="modal-patient-gender"></span></li>
              <li class="list-group-item"><strong>{{ __('messages.address') }}:</strong> <span id="modal-patient-address"></span></li>
              <li class="list-group-item"><strong>{{ __('messages.date_of_birth') }}:</strong> <span id="modal-patient-dob"></span></li>
              <li class="list-group-item"><strong>{{ __('messages.weight') }}:</strong> <span id="modal-patient-weight"></span> kg</li>
              <li class="list-group-item"><strong>{{ __('messages.height') }}:</strong> <span id="modal-patient-height"></span> cm</li>
              <li class="list-group-item"><strong>{{ __('messages.phone_number') }}:</strong> <span id="modal-patient-phone"></span></li>
              <li class="list-group-item"><strong>{{ __('messages.note') }}:</strong> <span id="modal-patient-note"></span></li>
            </ul>
          </div>
        </div>
      </div>
    </div>
@endsection
