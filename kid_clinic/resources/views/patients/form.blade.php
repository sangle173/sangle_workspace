<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card shadow-sm mb-4">
            <div class="card-body">
                <h4 class="text-center mb-4"><i class="fa-solid fa-user-injured me-2"></i> {{ isset($patient) ? __('messages.edit_patient') : __('messages.add_new_patient') }}</h4>
                <div class="row g-3">
                    <div class="col-md-6">
                        <div class="mb-3">
                            <label for="name" class="form-label">{{ __('messages.name') }}</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fa-solid fa-user"></i></span>
                                <input type="text" id="name" name="name" class="form-control @error('name') is-invalid @enderror" value="{{ old('name', $patient->name ?? '') }}">
                                @error('name')
                                <div class="invalid-feedback">{{ $message }}</div>
                                @enderror
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="gender" class="form-label">{{ __('messages.gender') }}</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fa-solid fa-venus-mars"></i></span>
                                <select id="gender" name="gender" class="form-select @error('gender') is-invalid @enderror" required>
                                    <option value="male" {{ old('gender', $patient->gender ?? '') == 'male' ? 'selected' : '' }}>{{ __('messages.male') }}</option>
                                    <option value="female" {{ old('gender', $patient->gender ?? '') == 'female' ? 'selected' : '' }}>{{ __('messages.female') }}</option>
                                </select>
                                @error('gender')
                                <div class="invalid-feedback">{{ $message }}</div>
                                @enderror
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="address_id" class="form-label">{{ __('messages.address') }}</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fa-solid fa-location-dot"></i></span>
                                <select id="address_id" name="address_id" class="form-select @error('address_id') is-invalid @enderror">
                                    <option value="">{{ __('messages.select_address') }}</option>
                                    @foreach($addresses as $address)
                                        <option value="{{ $address->id }}" {{ old('address_id', $patient->address_id ?? '') == $address->id ? 'selected' : '' }}>{{ $address->name }}</option>
                                    @endforeach
                                </select>
                                @error('address_id')
                                <div class="invalid-feedback">{{ $message }}</div>
                                @enderror
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="date_of_birth" class="form-label">{{ __('messages.date_of_birth') }}</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fa-solid fa-calendar-days"></i></span>
                                <input type="date" id="date_of_birth" name="date_of_birth" class="form-control @error('date_of_birth') is-invalid @enderror" value="{{ old('date_of_birth', $patient->date_of_birth ?? '') }}">
                                @error('date_of_birth')
                                <div class="invalid-feedback">{{ $message }}</div>
                                @enderror
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="mb-3">
                            <label for="weight" class="form-label">{{ __('messages.weight') }}</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fa-solid fa-weight-hanging"></i></span>
                                <input type="number" id="weight" name="weight" class="form-control @error('weight') is-invalid @enderror" value="{{ old('weight', $patient->weight ?? '') }}">
                                @error('weight')
                                <div class="invalid-feedback">{{ $message }}</div>
                                @enderror
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="height" class="form-label">{{ __('messages.height') }}</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fa-solid fa-ruler-vertical"></i></span>
                                <input type="number" id="height" name="height" class="form-control @error('height') is-invalid @enderror" value="{{ old('height', $patient->height ?? '') }}">
                                @error('height')
                                <div class="invalid-feedback">{{ $message }}</div>
                                @enderror
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="phone_number" class="form-label">{{ __('messages.phone_number') }}</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fa-solid fa-phone"></i></span>
                                <input type="text" id="phone_number" name="phone_number" class="form-control @error('phone_number') is-invalid @enderror" value="{{ old('phone_number', $patient->phone_number ?? '') }}">
                                @error('phone_number')
                                <div class="invalid-feedback">{{ $message }}</div>
                                @enderror
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="note" class="form-label">{{ __('messages.note') }}</label>
                            <textarea id="note" name="note" class="form-control" rows="2">{{ old('note', $patient->note ?? '') }}</textarea>
                        </div>
                    </div>
                </div>
                <div class="d-flex gap-2 justify-content-center mt-4">
                    <button type="submit" class="btn btn-success btn-sm"><i class="fa-solid fa-save me-1"></i> {{ __('messages.save') }}</button>
                    <a href="{{ route('patients.index') }}" class="btn btn-secondary btn-sm"><i class="fa-solid fa-arrow-left me-1"></i> {{ __('messages.back') }}</a>
                </div>
            </div>
        </div>
    </div>
</div>
