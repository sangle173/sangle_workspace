<?php

namespace Database\Seeders;

use App\Models\Patient;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class PatientSeeder extends Seeder
{
    public function run()
    {
        $patients = [
            [
                'name' => 'Nguyen Van A',
                'gender' => 'male',
                'address_id' => 1,
                'date_of_birth' => '2010-05-20',
                'weight' => 30.5,
                'height' => 120,
                'phone_number' => '0123456789',
                'note' => 'N/A',
                'status' => 1,
            ],
            [
                'name' => 'Tran Thi B',
                'gender' => 'female',
                'address_id' => 2,
                'date_of_birth' => '2012-11-15',
                'weight' => 25,
                'height' => 115,
                'phone_number' => '0987654321',
                'note' => 'Allergic to penicillin',
                'status' => 1,
            ],
        ];

        foreach ($patients as $patient) {
            Patient::create($patient);
        }
    }
}
