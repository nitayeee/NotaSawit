import os

controller_path = r"d:\Web PA\ProyekAkhir_Rini\app\Http\Controllers\Api\KegiatanController.php"
with open(controller_path, "r", encoding="utf-8") as f:
    content = f.read()

old_create = """        $kegiatan = Kegiatan::create([
            'petani_id'          => $request->petani_id,
            'jenis_kegiatan_id'  => $request->jenis_kegiatan_id,
            'kegiatan_tanggal'   => $request->kegiatan_tanggal,
            'kegiatan_jumlah'    => $request->kegiatan_jumlah,
            'kegiatan_satuan'    => $request->kegiatan_satuan,
            'kegiatan_ket'       => $request->kegiatan_ket,
        ]);"""

new_create = """        $kegiatan = Kegiatan::create([
            'petani_id'          => $request->petani_id,
            'jenis_kegiatan_id'  => $request->jenis_kegiatan_id,
            'kegiatan_tanggal'   => $request->kegiatan_tanggal,
            'kegiatan_jumlah'    => $request->kegiatan_jumlah,
            'kegiatan_satuan'    => $request->kegiatan_satuan,
            'kegiatan_ket'       => $request->kegiatan_ket ?? $request->nama_bahan,
            'nama_bahan'         => $request->nama_bahan ?? $request->kegiatan_ket,
            'jenis_limbah'       => $request->jenis_limbah,
            'status_limbah'      => $request->status_limbah ?? 'Belum Disetor',
        ]);"""

if old_create in content:
    content = content.replace(old_create, new_create)
    with open(controller_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("KegiatanController.php store method updated successfully!")
else:
    print("old_create pattern not found")
