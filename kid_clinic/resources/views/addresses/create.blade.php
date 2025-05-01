@extends('layouts.app')

@section('content')
<div class="row justify-content-center">
    <div class="col-lg-6">
        <div class="card shadow-sm mb-4">
            <div class="card-body">
                <h4 class="text-center mb-4"><i class="fa-solid fa-location-dot me-2"></i> {{ __('messages.add_new_address') }}</h4>
                <form action="{{ route('addresses.store') }}" method="POST">
                    @csrf
                    <div class="mb-3">
                        <label for="name" class="form-label">{{ __('messages.name') }}</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fa-solid fa-location-dot"></i></span>
                            <input type="text" id="name" name="name" class="form-control @error('name') is-invalid @enderror" value="{{ old('name') }}">
                            @error('name')
                            <div class="invalid-feedback">{{ $message }}</div>
                            @enderror
                        </div>
                    </div>
                    <div class="d-flex gap-2 justify-content-center mt-4">
                        <button type="submit" class="btn btn-success btn-sm"><i class="fa-solid fa-save me-1"></i> {{ __('messages.save') }}</button>
                        <a href="{{ route('addresses.index') }}" class="btn btn-secondary btn-sm"><i class="fa-solid fa-arrow-left me-1"></i> {{ __('messages.back') }}</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
@endsection
