<?php

namespace Database\Seeders;

use App\Models\User;
// use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Seed the application's database.
     */
    public function run()
    {
        $this->call([
            AdminUserSeeder::class,
            MedicineCategorySeeder::class,
            MedicineStatusSeeder::class,
            UnitSeeder::class,
            BrandSeeder::class,
            AddressSeeder::class,
            PatientSeeder::class,
            MedicineSeeder::class,
        ]);
    }
}
