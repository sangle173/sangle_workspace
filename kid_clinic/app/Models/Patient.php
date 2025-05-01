<?php

namespace App\Models;

class Patient extends BaseModel
{
    protected $fillable = [
        'name',
        'gender',
        'address_id',
        'date_of_birth',
        'weight',
        'height',
        'phone_number',
        'note',
        'status',
    ];

    public function address()
    {
        return $this->belongsTo(Address::class);
    }

    public function examineHistories()
    {
        return $this->hasMany(ExamineHistory::class);
    }
    // Scopes
    public $timestamps = true; // Enables automatic timestamps

    public function scopeActive($query)
    {
        return $query->where('status', 1);
    }
}

