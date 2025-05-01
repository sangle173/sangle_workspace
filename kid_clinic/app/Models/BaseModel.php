<?php


namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class BaseModel extends Model
{
    // Scope for active records (status = 1)
    public function scopeActive($query)
    {
        return $query->where('status', 1);
    }
}
