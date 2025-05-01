@extends('layouts.app')

@section('content')
    <h1>Edit Medicine Status</h1>
    <form action="{{ route('medicine-statuses.update', $medicineStatus->id) }}" method="POST">
        @csrf
        @method('PUT')
        @include('medicine_statuses.form', ['status' => $medicineStatus])
    </form>
@endsection
