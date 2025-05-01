@extends('layouts.app')

@section('content')
    <h1>Edit Unit</h1>
    <form action="{{ route('units.update', $unit->id) }}" method="POST">
        @csrf
        @method('PUT')
        @include('units.form', ['unit' => $unit])
    </form>
@endsection
