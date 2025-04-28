<form action="{{ $action }}" method="POST">
    @csrf
    @if(isset($method) && $method === 'PUT')
        @method('PUT')
    @endif

    <div class="mb-3">
        <label for="name" class="form-label">Notebook Name</label> {{-- 🔥 changed from Title to Name --}}
        <input type="text" 
               id="name" 
               name="name" 
               class="form-control @error('name') is-invalid @enderror" 
               value="{{ old('name', $notebook->name ?? '') }}" 
               required>

        @error('name')
            <div class="invalid-feedback">
                {{ $message }}
            </div>
        @enderror
    </div>

    <div class="d-grid">
        <button type="submit" class="btn btn-success">
            {{ $buttonText ?? 'Save' }}
        </button>
    </div>
</form>
