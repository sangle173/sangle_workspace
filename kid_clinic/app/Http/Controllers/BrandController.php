<?php

namespace App\Http\Controllers;

use App\Models\Brand;
use Illuminate\Http\Request;

class BrandController extends Controller
{
    public function index()
    {
        $brands = Brand::where('status', 1)->paginate(10); // Fetch only active brands
        return view('brands.index', compact('brands'));
    }

    public function create()
    {
        return view('brands.create');
    }

    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|string|max:255',
            'status' => 'required|boolean',
        ]);

        Brand::create($request->all());

        return redirect()->route('brands.index')->with('success', 'Brand created successfully.');
    }

    public function edit(Brand $brand)
    {
        return view('brands.edit', compact('brand'));
    }

    public function update(Request $request, Brand $brand)
    {
        $request->validate([
            'name' => 'required|string|max:255',
            'status' => 'required|boolean',
        ]);

        $brand->update($request->all());

        return redirect()->route('brands.index')->with('success', 'Brand updated successfully.');
    }

    public function destroy(Brand $brand)
    {
        $brand->update(['status' => 0]); // Set the status to 0 (inactive)
        return redirect()->route('brands.index')->with('success', 'Brand soft deleted successfully.');
    }

    public function restore($id)
    {
        $brand = Brand::findOrFail($id);
        $brand->update(['status' => 1]); // Set the status back to active (1)
        return redirect()->route('brands.index')->with('success', 'Brand restored successfully.');
    }
}
