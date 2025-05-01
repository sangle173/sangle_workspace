<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Medicine extends Model
{
    use HasFactory;

    protected $fillable = [
        'name',
        'quantity',
        'price', // Add this
        'brand_id', // Add this
        'unit_id',
        'medicine_status_id',
        'category_id',
        'image',
        'manufacture_date',
        'expired_date',
        'status',
    ];
    protected $casts = [
        'manufacture_date' => 'datetime', // Cast to Carbon instance
        'expired_date' => 'datetime', // Cast to Carbon instance
    ];

    /**
     * Relationship with Brand.
     */
    public function brand()
    {
        return $this->belongsTo(Brand::class, 'brand_id');
    }


    // Relationships
    public function unit()
    {
        return $this->belongsTo(Unit::class);
    }

    public function medicineStatus()
    {
        return $this->belongsTo(MedicineStatus::class);
    }

    public function category()
    {
        return $this->belongsTo(MedicineCategory::class);
    }

    // Scopes
    public function scopeActive($query)
    {
        return $query->where('status', 1);
    }
}


