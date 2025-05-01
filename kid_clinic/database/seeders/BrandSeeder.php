<?php

namespace Database\Seeders;

use App\Models\Brand;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class BrandSeeder extends Seeder
{
    public function run()
    {
        $brands = [
            ['name' => 'Pfizer', 'status' => 1],
            ['name' => 'Johnson & Johnson', 'status' => 1],
            ['name' => 'Novartis', 'status' => 1],
        ];

        foreach ($brands as $brand) {
            Brand::create($brand);
        }
    }
}
