<?php

namespace App\Http\Controllers;

use App\Models\Patient;
use App\Models\Address;
use Illuminate\Http\Request;

class PatientController extends Controller
{
    /**
     * Display a listing of patients.
     */
    public function index(Request $request)
    {
        // Get search keyword and selected address
        $search = $request->input('search');
        $selectedAddress = $request->input('address_id');

        // Get all addresses for the dropdown
        $addresses = Address::all();

        // Query patients with search and filter
        $patients = Patient::query()
            ->with('address')
            ->when($search, function ($query, $search) {
                return $query->where('name', 'like', '%' . $search . '%');
            })
            ->when($selectedAddress, function ($query, $selectedAddress) {
                return $query->where('address_id', $selectedAddress);
            })
            ->orderBy('id', 'desc')
            ->paginate(10);

        return view('patients.index', compact('patients', 'search', 'addresses', 'selectedAddress'));
    }


    /**
     * Show the form for creating a new patient.
     */
    public function create()
    {
        $addresses = Address::active()->get();
        return view('patients.create', compact('addresses'));
    }

    /**
     * Store a newly created patient in the database.
     */
    public function store(Request $request)
    {
        $validatedData = $request->validate([
            'name' => 'required|string|max:255',
            'gender' => 'required|in:male,female',
            'address_id' => 'required|exists:addresses,id',
            'date_of_birth' => 'required|date',
            'weight' => 'nullable|numeric|min:0',
            'height' => 'nullable|numeric|min:0',
            'phone_number' => 'nullable|string|max:20',
            'note' => 'nullable|string',
        ]);

        // Normalize the name
        $validatedData['name'] = $this->normalizeName($request->name);

        // Create the patient
        Patient::create($validatedData);

        return redirect()->route('patients.index')->with('success', __('messages.patient_created'));
    }


    /**
     * Show the form for editing the specified patient.
     */
    public function edit($id)
    {
        $patient = Patient::findOrFail($id); // Use `id` to find the patient
        $addresses = Address::active()->get();
        return view('patients.edit', compact('patient', 'addresses'));
    }

    /**
     * Update the specified patient in the database.
     */
    public function update(Request $request, Patient $patient)
    {
        $validatedData = $request->validate([
            'name' => 'required|string|max:255',
            'gender' => 'required|in:male,female',
            'address_id' => 'required|exists:addresses,id',
            'date_of_birth' => 'required|date',
            'weight' => 'nullable|numeric|min:0',
            'height' => 'nullable|numeric|min:0',
            'phone_number' => 'nullable|string|max:20',
            'note' => 'nullable|string',
        ]);

        // Normalize the name
        $validatedData['name'] = $this->normalizeName($request->name);

        // Update the patient
        $patient->update($validatedData);

        return redirect()->route('patients.index')->with('success', __('messages.patient_updated'));
    }


    /**
     * Remove the specified patient (soft delete).
     */
    public function destroy($id)
    {
        $patient = Patient::findOrFail($id);
        $patient->update(['status' => 0]); // Soft delete
        return redirect()->route('patients.index')->with('success', 'Patient deleted successfully.');
    }


    private function normalizeName($name)
    {
        // Trim extra spaces
        $name = trim($name);

        // Replace multiple spaces with a single space
        $name = preg_replace('/\s+/', ' ', $name);

        // Capitalize each word
        return ucwords(strtolower($name));
    }

}

