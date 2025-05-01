@extends('layouts.app')

@section('content')
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="mb-0">{{ __('messages.examine_histories') }}: {{ $patient->name }}</h1>
        <div>
            <!-- Add New Examine History Button -->
            <a href="{{ route('examine-histories.create', ['patient_id' => $patient->id]) }}" class="btn btn-primary">
                {{ __('messages.add_new') }} {{ __('messages.examine_histories') }}
            </a>
            <!-- Back to Patients Button -->
            <a href="{{ route('patients.index') }}" class="btn btn-secondary">
                {{ __('messages.back_to') }} {{ __('messages.patients') }}
            </a>
        </div>
    </div>

    <!-- Examine Histories Table -->
    <table class="table table-bordered">
        <thead>
        <tr>
            <th>#</th>
            <th>{{ __('messages.diagnose') }}</th>
            <th>{{ __('messages.symptoms') }}</th>
            <th>{{ __('messages.prescription') }}</th>
            <th>{{ __('messages.fee') }}</th>
            <th>{{ __('messages.created_at') }}</th>
        </tr>
        </thead>
        <tbody>
        @forelse($examineHistories as $history)
            <tr>
                <td>{{ $loop->iteration }}</td>
                <td>
                    <ul>
                        @foreach(explode(',', $history->diagnose) as $diagnose)
                            <li>{{ trim($diagnose) }}</li>
                        @endforeach
                    </ul>
                </td>
                <td>
                    <ul>
                        @foreach(explode(',', $history->symptoms) as $symptom)
                            <li>{{ trim($symptom) }}</li>
                        @endforeach
                    </ul>
                </td>
                <td>{{ $history->prescription }}</td>
                <td>{{ number_format($history->fee, 2) }}</td>
                <td>{{ $history->created_at->format('d/m/Y H:i') }}</td>
            </tr>
        @empty
            <tr>
                <td colspan="6" class="text-center">{{ __('messages.no_results') }}</td>
            </tr>
        @endforelse
        </tbody>
    </table>
@endsection
