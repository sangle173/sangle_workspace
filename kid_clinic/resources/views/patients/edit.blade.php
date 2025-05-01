@extends('layouts.app')

@section('content')
{{--    <h1>{{ __('messages.edit_patient') }}</h1>--}}
    <form action="{{ route('patients.update', $patient->id) }}" method="POST">
        @csrf
        @method('PUT')
        @include('patients.form', ['patient' => $patient])
    </form>
@endsection
