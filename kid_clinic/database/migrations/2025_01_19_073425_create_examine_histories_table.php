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
        Schema::create('examine_histories', function (Blueprint $table) {
            $table->id();
            $table->text('diagnose');
            $table->text('symptoms');
            $table->json('prescription')->nullable();
            $table->bigInteger('fee')->default(0); // Add fee as BIGINT
            $table->text('note')->nullable();
            $table->foreignId('patient_id')->constrained('patients')->cascadeOnDelete();
            $table->boolean('status')->default(1); // Soft delete column
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('examine_histories');
    }
};
