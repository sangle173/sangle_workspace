@extends('layouts.app')

@section('content')
    <h1>Add New Medicine</h1>
    <form action="{{ route('medicines.store') }}" method="POST" enctype="multipart/form-data">
        @csrf
        @include('medicines.form', ['medicine' => null])
    </form>
@endsection
