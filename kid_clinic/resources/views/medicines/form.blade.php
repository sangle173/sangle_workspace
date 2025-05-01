<div class="mb-3">
    <label for="name" class="form-label">Name</label>
    <input type="text" id="name" name="name" class="form-control"
           value="{{ old('name', $medicine->name ?? '') }}" required>
</div>

<div class="mb-3">
    <label for="category_id" class="form-label">Category</label>
    <select id="category_id" name="category_id" class="form-select" required>
        <option value="">Select Category</option>
        @foreach($categories as $category)
            <option value="{{ $category->id }}" {{ old('category_id', $medicine->category_id ?? '') == $category->id ? 'selected' : '' }}>
                {{ $category->name }}
            </option>
        @endforeach
    </select>
</div>

<div class="mb-3">
    <label for="unit_id" class="form-label">Unit</label>
    <select id="unit_id" name="unit_id" class="form-select" required>
        <option value="">Select Unit</option>
        @foreach($units as $unit)
            <option value="{{ $unit->id }}" {{ old('unit_id', $medicine->unit_id ?? '') == $unit->id ? 'selected' : '' }}>
                {{ $unit->name }}
            </option>
        @endforeach
    </select>
</div>

<div class="mb-3">
    <label for="medicine_status_id" class="form-label">Medicine Status</label>
    <select id="medicine_status_id" name="medicine_status_id" class="form-select" required>
        <option value="">Select Status</option>
        @foreach($statuses as $status)
            <option value="{{ $status->id }}" {{ old('medicine_status_id', $medicine->medicine_status_id ?? '') == $status->id ? 'selected' : '' }}>
                {{ $status->name }}
            </option>
        @endforeach
    </select>
</div>

<div class="mb-3">
    <label for="brand_id" class="form-label">Brand</label>
    <select id="brand_id" name="brand_id" class="form-select">
        <option value="">Select Brand</option>
        @foreach($brands as $brand)
            <option value="{{ $brand->id }}" {{ old('brand_id', $medicine->brand_id ?? '') == $brand->id ? 'selected' : '' }}>
                {{ $brand->name }}
            </option>
        @endforeach
    </select>
</div>

<div class="mb-3">
    <label for="quantity" class="form-label">Quantity</label>
    <input type="number" id="quantity" name="quantity" class="form-control"
           value="{{ old('quantity', $medicine->quantity ?? 0) }}" required>
</div>
<div class="mb-3">
    <label for="price" class="form-label">Price</label>
    <input type="number" id="price" name="price" class="form-control"
           value="{{ old('price', isset($medicine) ? $medicine->price / 100 : '') }}"
           placeholder="Enter price in dollars (e.g., 10.00)" step="0.01" required>
</div>

<div class="mb-3">
    <label for="manufacture_date" class="form-label">Manufacture Date</label>
    <input type="date" id="manufacture_date" name="manufacture_date" class="form-control"
           value="{{ old('manufacture_date', $medicine->manufacture_date ?? '') }}">
</div>

<div class="mb-3">
    <label for="expired_date" class="form-label">Expired Date</label>
    <input type="date" id="expired_date" name="expired_date" class="form-control"
           value="{{ old('expired_date', $medicine->expired_date ?? '') }}">
</div>

<div class="mb-3">
    <label for="image" class="form-label">Image</label>
    @if(isset($medicine) && $medicine->image)
        <img id="preview-image" src="{{ asset('storage/' . $medicine->image) }}" alt="Medicine Image" width="100" class="d-block mb-2">
    @else
        <img id="preview-image" src="#" alt="Preview Image" width="100" class="d-block mb-2" style="display: none;">
    @endif
    <input type="file" id="image" name="image" class="form-control" onchange="previewImage(event)">
</div>

<div class="mb-3">
    <label for="status" class="form-label">Active Status</label>
    <select id="status" name="status" class="form-select">
        <option value="1" {{ old('status', $medicine->status ?? 1) == 1 ? 'selected' : '' }}>Active</option>
        <option value="0" {{ old('status', $medicine->status ?? 1) == 0 ? 'selected' : '' }}>Inactive</option>
    </select>
</div>

<button type="submit" class="btn btn-success">Save</button>
<a href="{{ route('medicines.index') }}" class="btn btn-secondary">Back</a>

<script>
    function previewImage(event) {
        const reader = new FileReader();
        reader.onload = function() {
            const output = document.getElementById('preview-image');
            output.src = reader.result;
            output.style.display = 'block';
        };
        reader.readAsDataURL(event.target.files[0]);
    }
</script>
