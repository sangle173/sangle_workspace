@extends('layouts.app')

@section('content')
    <h1>Edit Brand</h1>
    <form action="{{ route('brands.update', $brand->id) }}" method="POST">
        @csrf
        @method('PUT')
        @include('brands.form', ['brand' => $brand])
    </form>
@endsection
