<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Note;
use App\Models\Notebook;

class NoteSeeder extends Seeder
{
    public function run()
    {
        $notebook1 = Notebook::where('name', 'Sonos Notes')->first();
        $notebook2 = Notebook::where('name', 'English Improvement')->first();

        if ($notebook1) {
            Note::create([
                'notebook_id' => $notebook1->id,
                'title' => 'Setup ADB',
                'body' => 'How to setup ADB for Sonos app testing...'
            ]);

            Note::create([
                'notebook_id' => $notebook1->id,
                'title' => 'Verify Tickets',
                'body' => 'Checklist before verifying a ticket...'
            ]);
        }

        if ($notebook2) {
            Note::create([
                'notebook_id' => $notebook2->id,
                'title' => 'English Writing Tips',
                'body' => 'Practice writing daily using Grammarly...'
            ]);
        }
    }
}
