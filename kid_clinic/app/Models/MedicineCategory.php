<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class MedicineCategory extends Model
{
    use HasFactory;

    protected $fillable = ['name', 'status'];

    // Scope for active records
    public function scopeActive($query)
    {
        return $query->where('status', 1);
    }
}


