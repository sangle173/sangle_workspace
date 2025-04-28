<?php

namespace App\Http\Controllers;

use App\Models\Notebook;
use Illuminate\Http\Request;

class NotebookController extends Controller
{
    public function index()
    {
        $notebooks = Notebook::with('notes')->get();
        return view('notebooks.index', compact('notebooks'));
    }

    public function create()
    {
        $notebooks = Notebook::with('notes')->get(); // <-- Load here too!
        return view('notebooks.create', compact('notebooks'));
    }

    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|string|max:255',  // 🔥 validate 'name', not 'title'
        ]);

        Notebook::create($request->only('name'));

        return redirect()->route('notebooks.index');
    }

    public function edit(Notebook $notebook)
    {
        $notebooks = Notebook::with('notes')->get(); // <-- Load here too!
        return view('notebooks.edit', compact('notebook', 'notebooks'));
    }

    public function update(Request $request, Notebook $notebook)
    {
        $request->validate([
            'name' => 'required|string|max:255',
        ]);

        $notebook->update($request->only('name'));

        return redirect()->route('notebooks.index');
    }

    public function destroy(Notebook $notebook)
    {
        $notebook->delete();

        return redirect()->route('notebooks.index');
    }

    public function show(Notebook $notebook)
    {
        $notebooks = Notebook::with('notes')->get();
        return view('notebooks.show', compact('notebook', 'notebooks'));
    }
}

