<?php

namespace App\Http\Controllers;

use App\Models\Address;
use Illuminate\Http\Request;

class AddressController extends Controller
{
    /**
     * Display a listing of addresses.
     */
    public function index()
    {
        $addresses = Address::active()->paginate(10); // Fetch active (status = 1) addresses
        return view('addresses.index', compact('addresses'));
    }

    /**
     * Show the form for creating a new address.
     */
    public function create()
    {
        return view('addresses.create');
    }

    /**
     * Store a newly created address in the database.
     */
    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|string|max:255|unique:addresses,name',
        ]);

        Address::create($request->all());
        return redirect()->route('addresses.index')->with('success', 'Address created successfully.');
    }

    /**
     * Show the form for editing the specified address.
     */
    public function edit(Address $address)
    {
        return view('addresses.edit', compact('address'));
    }

    /**
     * Update the specified address in the database.
     */
    public function update(Request $request, Address $address)
    {
        $request->validate([
            'name' => 'required|string|max:255|unique:addresses,name,' . $address->id,
        ]);

        $address->update($request->all());
        return redirect()->route('addresses.index')->with('success', 'Address updated successfully.');
    }

    /**
     * Soft delete the specified address (set status = 0).
     */
    public function destroy(Address $address)
    {
        $address->update(['status' => 0]); // Soft delete
        return redirect()->route('addresses.index')->with('success', 'Address deleted successfully.');
    }
}

