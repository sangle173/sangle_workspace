<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class ExamineHistory extends Model
{
    use HasFactory;

    protected $fillable = [
        'diagnose',
        'symptoms',
        'prescription',
        'fee',
        'profit',
        'note',
        'patient_id',
        'status',
    ];

    // Relationships
    public function patient()
    {
        return $this->belongsTo(Patient::class);
    }

    // Scopes
    public function scopeActive($query)
    {
        return $query->where('status', 1);
    }
}

