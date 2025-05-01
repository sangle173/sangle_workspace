<?php

namespace Database\Seeders;

use App\Models\Medicine;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class MedicineSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run()
    {
        $medicines = [
            [
                'name' => 'Amoxicillin',
                'quantity' => 100,
                'price' => 1500,
                'category_id' => 1,
                'unit_id' => 1,
                'medicine_status_id' => 1,
                'brand_id' => 1,
                'manufacture_date' => '2024-01-01',
                'expired_date' => '2025-01-01',
                'status' => 1,
            ],
            [
                'name' => 'Paracetamol',
                'quantity' => 200,
                'price' => 2500,
                'category_id' => 2,
                'unit_id' => 2,
                'medicine_status_id' => 1,
                'brand_id' => 2,
                'manufacture_date' => '2024-05-01',
                'expired_date' => '2026-05-01',
                'status' => 1,
            ],
        ];

        foreach ($medicines as $medicine) {
            Medicine::create($medicine);
        }
    }
}
