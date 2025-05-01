<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Address extends Model
{
    use HasFactory;

    protected $fillable = [
        'name',
        'status',
    ];

    // Relationships
    public function patients()
    {
        return $this->hasMany(Patient::class);
    }

    // Scopes
    public function scopeActive($query)
    {
        return $query->where('status', 1);
    }
}
