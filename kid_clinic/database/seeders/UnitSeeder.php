<?php

namespace Database\Seeders;

use App\Models\Unit;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class UnitSeeder extends Seeder
{
    public function run()
    {
        $units = [
            ['name' => 'Tablet', 'status' => 1],
            ['name' => 'Capsule', 'status' => 1],
            ['name' => 'Bottle', 'status' => 1],
        ];

        foreach ($units as $unit) {
            Unit::create($unit);
        }
    }
}
