@extends('layouts.app')

@section('content')
    <h1>Add New Unit</h1>
    <form action="{{ route('units.store') }}" method="POST">
        @csrf
        @include('units.form', ['unit' => null])
    </form>
@endsection
