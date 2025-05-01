<?php

namespace Database\Seeders;

use App\Models\MedicineCategory;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class MedicineCategorySeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run()
    {
        $categories = [
            ['name' => 'Antibiotics', 'status' => 1],
            ['name' => 'Pain Relievers', 'status' => 1],
            ['name' => 'Vitamins', 'status' => 1],
        ];

        foreach ($categories as $category) {
            MedicineCategory::create($category);
        }
    }
}
