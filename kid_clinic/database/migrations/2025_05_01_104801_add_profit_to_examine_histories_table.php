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
        Schema::table('examine_histories', function (Blueprint $table) {
            $table->bigInteger('profit')->nullable()->after('fee');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('examine_histories', function (Blueprint $table) {
            $table->dropColumn('profit');
        });
    }
};
