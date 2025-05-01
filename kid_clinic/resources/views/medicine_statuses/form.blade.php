<div class="mb-3">
    <label for="name" class="form-label">Name</label>
    <input type="text" id="name" name="name" class="form-control"
           value="{{ old('name', $status->name ?? '') }}" required>
</div>

<div class="mb-3">
    <label for="status" class="form-label">Status</label>
    <select id="status" name="status" class="form-select">
        <option value="1" {{ old('status', $status->status ?? 1) == 1 ? 'selected' : '' }}>Active</option>
        <option value="0" {{ old('status', $status->status ?? 1) == 0 ? 'selected' : '' }}>Inactive</option>
    </select>
</div>

<button type="submit" class="btn btn-success">Save</button>
<a href="{{ route('medicine-statuses.index') }}" class="btn btn-secondary">Back</a>
