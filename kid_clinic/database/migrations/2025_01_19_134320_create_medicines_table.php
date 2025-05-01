<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('medicines', function (Blueprint $table) {
            $table->id();
            $table->string('name');
            $table->integer('quantity');
            $table->foreignId('unit_id')->constrained('units')->cascadeOnDelete();
            $table->foreignId('medicine_status_id')->constrained('medicine_statuses')->cascadeOnDelete();
            $table->foreignId('category_id')->constrained('medicine_categories')->cascadeOnDelete();
            $table->foreignId('brand_id')->constrained('brands')->cascadeOnDelete();
            $table->string('image')->nullable();
            $table->unsignedBigInteger('price')->nullable()->comment('Price in smallest currency unit (e.g., cents)');
            $table->dateTime('manufacture_date');
            $table->dateTime('expired_date');
            $table->boolean('status')->default(1); // Soft delete column
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('medicines');
    }
};
