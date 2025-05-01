@extends('layouts.app')

@section('content')
    <h1>Add New Medicine Status</h1>
    <form action="{{ route('medicine-statuses.store') }}" method="POST">
        @csrf
        @include('medicine_statuses.form', ['status' => null])
    </form>
@endsection
