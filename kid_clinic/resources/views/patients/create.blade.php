@extends('layouts.app')

@section('content')
{{--    <h1>{{ __('messages.add_new_patient') }}</h1>--}}
    <form action="{{ route('patients.store') }}" method="POST">
        @csrf
        @include('patients.form', ['patient' => null])
    </form>
@endsection
