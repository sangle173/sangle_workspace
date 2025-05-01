<?php

namespace App\Http\Controllers;

use App\Models\Brand;
use App\Models\Medicine;
use App\Models\MedicineCategory;
use App\Models\Unit;
use App\Models\MedicineStatus;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class MedicineController extends Controller
{
    /**
     * Display a listing of the medicines.
     */
    public function index(Request $request)
    {
        // Get search and filter inputs
        $search = $request->input('search');
        $categoryId = $request->input('category_id');
        $statusId = $request->input('medicine_status_id');
        $brandId = $request->input('brand_id');
        $unitId = $request->input('unit_id');

        // Query the medicines
        $medicines = Medicine::query()
            ->with(['category', 'unit', 'medicineStatus', 'brand'])
            ->when($search, function ($query, $search) {
                $query->where('name', 'like', '%' . $search . '%');
            })
            ->when($categoryId, function ($query, $categoryId) {
                $query->where('category_id', $categoryId);
            })
            ->when($statusId, function ($query, $statusId) {
                $query->where('medicine_status_id', $statusId);
            })
            ->when($brandId, function ($query, $brandId) {
                $query->where('brand_id', $brandId);
            })
            ->when($unitId, function ($query, $unitId) {
                $query->where('unit_id', $unitId);
            })
            ->paginate(10);

        // Get categories, statuses, brands, and units for filters
        $categories = MedicineCategory::active()->get();
        $statuses = MedicineStatus::active()->get();
        $brands = Brand::active()->get();
        $units = Unit::active()->get();

        return view('medicines.index', compact('medicines', 'categories', 'statuses', 'brands', 'units', 'search', 'categoryId', 'statusId', 'brandId', 'unitId'));
    }


    /**
     * Show the form for creating a new medicine.
     */
    public function create()
    {
        $categories = MedicineCategory::active()->get();
        $units = Unit::active()->get();
        $statuses = MedicineStatus::active()->get();
        $brands = Brand::where('status', 1)->get(); // Only active brands
        return view('medicines.create', compact('categories', 'units', 'statuses', 'brands'));
    }


    /**
     * Store a newly created medicine in storage.
     */
    public function store(Request $request)
    {
//        dd($request);
        $request->validate([
            'name' => 'required|string|max:255',
            'brand_id' => 'required|exists:brands,id',
            'category_id' => 'required|exists:medicine_categories,id',
            'unit_id' => 'required|exists:units,id',
            'medicine_status_id' => 'required|exists:medicine_statuses,id',
            'quantity' => 'required|integer|min:0',
            'price' => 'required|numeric|min:0', // Validate price in dollars
            'manufacture_date' => 'nullable|date',
            'expired_date' => 'nullable|date',
            'image' => 'nullable|image|mimes:jpeg,png,jpg,gif|max:2048', // Validate the image
            'status' => 'required|boolean',
        ]);

        $data = $request->all();

        // Handle image upload
        if ($request->hasFile('image')) {
            $data['image'] = $request->file('image')->store('medicines', 'public');
        }

        Medicine::create($data);

        return redirect()->route('medicines.index')->with('success', 'Medicine created successfully.');
    }

    /**
     * Show the form for editing the specified medicine.
     */
    public function edit(Medicine $medicine)
    {
        $categories = MedicineCategory::active()->get();
        $units = Unit::active()->get();
        $statuses = MedicineStatus::active()->get();
        $brands = Brand::where('status', 1)->get(); // Only active brands
        return view('medicines.edit', compact('medicine', 'categories', 'units', 'statuses', 'brands'));
    }

    /**
     * Update the specified medicine in storage.
     */
    public function update(Request $request, Medicine $medicine)
    {
        $request->validate([
            'name' => 'required|string|max:255',
            'brand_id' => 'required|exists:brands,id',
            'category_id' => 'required|exists:medicine_categories,id',
            'unit_id' => 'required|exists:units,id',
            'medicine_status_id' => 'required|exists:medicine_statuses,id',
            'quantity' => 'required|integer|min:0',
            'price' => 'required|numeric|min:0', // Validate price in dollars
            'manufacture_date' => 'nullable|date',
            'expired_date' => 'nullable|date',
            'image' => 'nullable|image|mimes:jpeg,png,jpg,gif|max:2048', // Validate the image
            'status' => 'required|boolean',
        ]);

        $data = $request->all();

        // Handle image upload
        if ($request->hasFile('image')) {
            // Delete old image if exists
            if ($medicine->image) {
                Storage::disk('public')->delete($medicine->image);
            }

            // Store new image
            $data['image'] = $request->file('image')->store('medicines', 'public');
        }

        $medicine->update($data);

        return redirect()->route('medicines.index')->with('success', 'Medicine updated successfully.');
    }

    /**
     * Remove the specified medicine from storage (soft delete).
     */
    public function destroy(Medicine $medicine)
    {
        // Delete the image if it exists
        if ($medicine->image) {
            Storage::disk('public')->delete($medicine->image);
        }

        $medicine->delete();

        return redirect()->route('medicines.index')->with('success', 'Medicine deleted successfully.');
    }
}


