<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Tag;
use App\Models\Note;

class TagSeeder extends Seeder
{
    public function run()
    {
        $urgent = Tag::create(['name' => 'urgent']);
        $verify = Tag::create(['name' => 'ready verify']);

        $note1 = Note::where('title', 'Setup ADB')->first();
        $note2 = Note::where('title', 'Verify Tickets')->first();

        if ($note1) {
            $note1->tags()->attach($urgent->id);
        }

        if ($note2) {
            $note2->tags()->attach($verify->id);
        }
    }
}
