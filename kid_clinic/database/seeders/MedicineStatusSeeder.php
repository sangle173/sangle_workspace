<?php

namespace Database\Seeders;

use App\Models\MedicineStatus;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class MedicineStatusSeeder extends Seeder
{
    public function run()
    {
        $statuses = [
            ['name' => 'Available', 'status' => 1],
            ['name' => 'Out of Stock', 'status' => 1],
            ['name' => 'Discontinued', 'status' => 1],
        ];

        foreach ($statuses as $status) {
            MedicineStatus::create($status);
        }
    }
}
