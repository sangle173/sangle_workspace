@extends('layouts.app')

@section('content')
    <h1>Edit Medicine</h1>
    <form action="{{ route('medicines.update', $medicine->id) }}" method="POST" enctype="multipart/form-data">
        @csrf
        @method('PUT')
        @include('medicines.form', ['medicine' => $medicine])
    </form>
@endsection
