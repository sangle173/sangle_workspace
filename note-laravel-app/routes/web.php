<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\NotebookController;
use App\Http\Controllers\NoteController;

Route::get('/', [NotebookController::class, 'index'])->name('home');

Route::resource('notebooks', NotebookController::class);
Route::resource('notes', NoteController::class);

// TinyMCE image upload
Route::post('/notes/upload-image', [App\Http\Controllers\NoteController::class, 'uploadImage'])->name('notes.uploadImage');
// Route
Route::put('/notes/{note}/update-title', [NoteController::class, 'updateTitle'])->name('notes.updateTitle');
