<?php

namespace Database\Seeders;

use App\Models\Address;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class AddressSeeder extends Seeder
{
    public function run()
    {
        $addresses = [
            ['name' => 'Hanoi', 'status' => 1],
            ['name' => 'Ho Chi Minh City', 'status' => 1],
            ['name' => 'Da Nang', 'status' => 1],
        ];

        foreach ($addresses as $address) {
            Address::create($address);
        }
    }
}
