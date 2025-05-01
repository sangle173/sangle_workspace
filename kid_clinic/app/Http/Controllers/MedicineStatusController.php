<?php

namespace App\Http\Controllers;

use App\Models\MedicineStatus;
use Illuminate\Http\Request;

class MedicineStatusController extends Controller
{
    public function index()
    {
        $medicineStatuses  = MedicineStatus::active()->paginate(10);
        return view('medicine_statuses.index', compact('medicineStatuses'));
    }

    public function create()
    {
        return view('medicine_statuses.create');
    }

    public function store(Request $request)
    {
        $request->validate(['name' => 'required|string|max:255']);
        MedicineStatus::create($request->all());
        return redirect()->route('medicine-statuses.index')->with('success', 'Medicine Status created successfully.');
    }

    public function edit(MedicineStatus $medicineStatus)
    {
        return view('medicine_statuses.edit', compact('medicineStatus'));
    }

    public function update(Request $request, MedicineStatus $medicineStatus)
    {
        $request->validate(['name' => 'required|string|max:255']);
        $medicineStatus->update($request->all());
        return redirect()->route('medicine-statuses.index')->with('success', 'Medicine Status updated successfully.');
    }

    public function destroy(MedicineStatus $medicineStatus)
    {
        $medicineStatus->update(['status' => 0]); // Soft delete
        return redirect()->route('medicine-statuses.index')->with('success', 'Medicine Status deleted successfully.');
    }
}

