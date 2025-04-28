@extends('layouts.app')

@section('content')
<div class="container-fluid">
    <h2>Create Notebook</h2>

    @include('notebooks._form', [
        'action' => route('notebooks.store'),
        'method' => 'POST',
        'notebook' => new \App\Models\Notebook(), // pass empty model
        'buttonText' => 'Create'
    ])
</div>
@endsection
