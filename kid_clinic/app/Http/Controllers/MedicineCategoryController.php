<?php

namespace App\Http\Controllers;

use App\Models\MedicineCategory;
use Illuminate\Http\Request;

class MedicineCategoryController extends Controller
{
    /**
     * Display a listing of medicine categories.
     */
    public function index()
    {
        $categories = MedicineCategory::active()->paginate(10);
        return view('medicine_categories.index', compact('categories'));
    }

    /**
     * Show the form for creating a new medicine category.
     */
    public function create()
    {
        return view('medicine_categories.create');
    }

    /**
     * Store a newly created medicine category in the database.
     */
    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|string|max:255|unique:medicine_categories,name',
        ]);

        MedicineCategory::create($request->all());
        return redirect()->route('medicine-categories.index')->with('success', 'Medicine category created successfully.');
    }

    /**
     * Show the form for editing the specified medicine category.
     */
    public function edit(MedicineCategory $medicineCategory)
    {
        return view('medicine_categories.edit', compact('medicineCategory'));
    }

    /**
     * Update the specified medicine category in the database.
     */
    public function update(Request $request, MedicineCategory $medicineCategory)
    {
        $request->validate([
            'name' => 'required|string|max:255|unique:medicine_categories,name,' . $medicineCategory->id,
        ]);

        $medicineCategory->update($request->all());
        return redirect()->route('medicine-categories.index')->with('success', 'Medicine category updated successfully.');
    }

    /**
     * Soft delete the specified medicine category (set status = 0).
     */
    public function destroy(MedicineCategory $medicineCategory)
    {
        $medicineCategory->update(['status' => 0]); // Soft delete
        return redirect()->route('medicine-categories.index')->with('success', 'Medicine category deleted successfully.');
    }
}

