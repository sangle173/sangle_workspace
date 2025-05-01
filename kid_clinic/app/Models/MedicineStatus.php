<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class MedicineStatus extends Model
{
    use HasFactory;

    protected $fillable = [
        'name',
        'status',
    ];

    // Relationships
    public function medicines()
    {
        return $this->hasMany(Medicine::class);
    }

    // Scopes
    public function scopeActive($query)
    {
        return $query->where('status', 1);
    }
}



