import os

migration_path = r"d:\Web PA\ProyekAkhir_Rini\database\migrations\2026_09_02_000000_add_bahan_and_limbah_to_kegiatan_table.php"

migration_code = """<?php

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
        Schema::table('kegiatan', function (Blueprint $table) {
            if (!Schema::hasColumn('kegiatan', 'nama_bahan')) {
                $table->string('nama_bahan')->nullable();
            }
            if (!Schema::hasColumn('kegiatan', 'jenis_limbah')) {
                $table->string('jenis_limbah')->nullable();
            }
            if (!Schema::hasColumn('kegiatan', 'status_limbah')) {
                $table->string('status_limbah')->default('Belum Disetor');
            }
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('kegiatan', function (Blueprint $table) {
            $table->dropColumn(['nama_bahan', 'jenis_limbah', 'status_limbah']);
        });
    }
};
"""

with open(migration_path, "w", encoding="utf-8") as f:
    f.write(migration_code)

print("Migration file created successfully at:", migration_path)
