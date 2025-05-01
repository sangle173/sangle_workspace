@extends('layouts.app')

@section('content')
    <h1>Add New Brand</h1>
    <form action="{{ route('brands.store') }}" method="POST">
        @csrf
        @include('brands.form', ['brand' => null])
    </form>
@endsection
