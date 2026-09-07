import os

# 1. Update app/Models/Kegiatan.php
model_path = r"d:\Web PA\ProyekAkhir_Rini\app\Models\Kegiatan.php"
if os.path.exists(model_path):
    with open(model_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    if "'nama_kegiatan'" not in content:
        content = content.replace(
            "'petani_id',",
            "'petani_id',\n        'nama_kegiatan',"
        )
        with open(model_path, "w", encoding="utf-8") as f:
            f.write(content)
        print("Model Kegiatan.php updated with nama_kegiatan!")

# 2. Update app/Http/Controllers/Api/KegiatanController.php
controller_path = r"d:\Web PA\ProyekAkhir_Rini\app\Http\Controllers\Api\KegiatanController.php"
if os.path.exists(controller_path):
    with open(controller_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Update validate rules in store()
    old_val = "'jenis_kegiatan_id'  => 'required|integer',"
    new_val = "'jenis_kegiatan_id'  => 'nullable|integer',\n            'nama_kegiatan'      => 'nullable|string',"

    if old_val in content and "'nama_kegiatan'" not in content:
        content = content.replace(old_val, new_val)

    # Update Kegiatan::create in store()
    old_create = "'jenis_kegiatan_id'  => $request->jenis_kegiatan_id,"
    new_create = "'jenis_kegiatan_id'  => $request->jenis_kegiatan_id ?? 0,\n            'nama_kegiatan'      => $request->nama_kegiatan ?? $request->jenis_kegiatan_nama,"

    if old_create in content and "'nama_kegiatan'" not in content:
        content = content.replace(old_create, new_create)

    with open(controller_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("KegiatanController.php updated with nama_kegiatan!")

# 3. Update migration file
migration_path = r"d:\Web PA\ProyekAkhir_Rini\database\migrations\2026_09_02_000000_add_bahan_and_limbah_to_kegiatan_table.php"
if os.path.exists(migration_path):
    with open(migration_path, "r", encoding="utf-8") as f:
        m_content = f.read()
    
    if "nama_kegiatan" not in m_content:
        m_content = m_content.replace(
            "Schema::table('kegiatan', function (Blueprint $table) {",
            "Schema::table('kegiatan', function (Blueprint $table) {\n            if (!Schema::hasColumn('kegiatan', 'nama_kegiatan')) {\n                $table->string('nama_kegiatan')->nullable();\n            }"
        )
        with open(migration_path, "w", encoding="utf-8") as f:
            f.write(m_content)
        print("Migration updated with nama_kegiatan!")
