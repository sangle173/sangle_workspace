@extends('layouts.app')

@section('content')
<div class="container-fluid">
    <h2>Edit Notebook</h2>

    @include('notebooks._form', [
        'action' => route('notebooks.update', $notebook),
        'method' => 'PUT',
        'notebook' => $notebook,
        'buttonText' => 'Update'
    ])
</div>
@endsection
