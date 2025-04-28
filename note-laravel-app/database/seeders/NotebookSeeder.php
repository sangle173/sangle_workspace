<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Notebook;

class NotebookSeeder extends Seeder
{
    public function run()
    {
        Notebook::create(['name' => 'Sonos Notes']);
        Notebook::create(['name' => 'English Improvement']);
    }
}
