<?php

namespace App\Http\Controllers;

use App\Models\Notebook;
use App\Models\Note;
use Illuminate\Http\Request;

class NoteController extends Controller
{
    public function create(Request $request)
    {
        $notebooks = Notebook::all();
        $preselectedNotebookId = $request->input('notebook_id'); // ✅
    
        return view('notes.create', compact('notebooks', 'preselectedNotebookId'));
    }
    

    public function store(Request $request)
{
    $request->validate([
        'notebook_id' => 'required|exists:notebooks,id',
        'title' => 'required|string|max:255',
        'content' => 'nullable',
    ]);

    Note::create([
        'notebook_id' => $request->notebook_id,
        'title' => $request->title,
        'body' => $request->content,  // ✅ map content ➔ body
    ]);

    return redirect()->route('home')->with('success', 'Note created!');
}


    public function edit(Note $note)
    {
        $notebooks = Notebook::all();
        return view('notes.edit', compact('note', 'notebooks'));
    }

    public function update(Request $request, Note $note)
{
    $request->validate([
        'notebook_id' => 'required|exists:notebooks,id',
        'title' => 'required|string|max:255',
        'content' => 'nullable',
    ]);

    $note->update([
        'notebook_id' => $request->notebook_id,
        'title' => $request->title,
        'body' => $request->content,  // ✅ map content ➔ body
    ]);

    return redirect()->route('home')->with('success', 'Note updated!');
}


    public function destroy(Note $note)
    {
        $note->delete();
        return redirect()->route('home')->with('success', 'Note deleted!');
    }

    public function uploadImage(Request $request)
{
    if ($request->hasFile('file')) {
        $file = $request->file('file');
        $filename = uniqid() . '.' . $file->getClientOriginalExtension();
        $path = $file->storeAs('uploads', $filename, 'public');

        return response()->json([
            'location' => asset('storage/' . $path),
        ]);
    }

    return response()->json(['error' => 'No file uploaded.'], 400);
}
public function updateTitle(Request $request, Note $note)
{
    $request->validate([
        'title' => 'required|string|max:255',
    ]);

    $note->update([
        'title' => $request->title,
    ]);

    return redirect()->back()->with('success', 'Title updated!');
}

}
