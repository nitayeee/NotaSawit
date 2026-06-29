package com.example.notasawit.RiwayatKeuangan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.DetailRiwayatKeuangan.RiwayatPemasukanActivity
import com.example.notasawit.DetailRiwayatKeuangan.RiwayatPengeluaranActivity
import com.example.notasawit.Model.Lahan
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityRiwayatBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest

class RiwayatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatBinding
    private lateinit var adapter: RiwayatAdapter

    private val riwayatList  = mutableListOf<RiwayatItem>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }

    private val sp_petaniId by lazy { sharedPref.getInt("petani_id", 0) }

    // Filter
    private var selectedTipe = "semua"
    private var selectedBulan: Int? = null
    private var selectedTahun: Int? = null
    private val lahanList = mutableListOf<Lahan>()
    private var selectedLahanId: Int? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityRiwayatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        loadLahan()
        loadRiwayat()
        binding.spinnerLahan.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    selectedLahanId =
                        if(position == 0) {
                            null
                        } else {
                            lahanList[position - 1].lahan_id
                        }

                    loadRiwayat()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        adapter = RiwayatAdapter(riwayatList.toMutableList())


        adapter.onItemClick = { item ->

            when(item.sourceTable.lowercase()) {


                "produksi" -> {

                    val intent = Intent(
                        this,
                        RiwayatPemasukanActivity::class.java
                    )

                    intent.putExtra(
                        "produksi_id",
                        item.id
                    )

                    startActivity(intent)
                }
                "biaya_operasional" -> {

                    val intent = Intent(
                        this,
                        RiwayatPengeluaranActivity::class.java
                    )
                    intent.putExtra(
                        "biaya_id",
                        item.id
                    )

                    startActivity(intent)
                }
            }
        }
        binding.rvTransaksi.adapter = adapter

        binding.rvTransaksi.layoutManager =
            LinearLayoutManager(this)

        binding.tabLayoutFilter.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab?) {

                    selectedTipe = when(tab?.position) {
                        1 -> "pemasukan"
                        2 -> "pengeluaran"
                        else -> "semua"
                    }
                    loadRiwayat()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            }
        )
        binding.tvPeriode.setOnClickListener {

            showFilterPeriode()
        }
    }
    private fun loadLahan() {

        PetaniApi.getLahanByPetani(
            sp_petaniId,
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val jsonString =
                        response.body?.string() ?: return

                    val json =
                        JSONObject(jsonString)

                    val data =
                        json.getJSONArray("data")

                    lahanList.clear()

                    val namaLahan =
                        mutableListOf("Semua Lahan")

                    for (i in 0 until data.length()) {

                        val obj =
                            data.getJSONObject(i)

                        val lahan =
                            Lahan(
                                lahan_id =
                                    obj.getInt("lahan_id"),
                                lahan_nama =
                                    obj.getString("lahan_nama")
                            )

                        lahanList.add(lahan)

                        namaLahan.add(
                            lahan.lahan_nama
                        )
                    }

                    runOnUiThread {

                        val adapter =
                            ArrayAdapter(
                                this@RiwayatActivity,
                                android.R.layout.simple_spinner_item,
                                namaLahan
                            )

                        adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )

                        binding.spinnerLahan.adapter =
                            adapter
                    }
                }
            }
        )
    }
    private fun loadRiwayat(
        tipe: String = "semua"
    ) {

        val petaniId =sp_petaniId

        PetaniApi.getRiwayatKeuangan(
            petaniId = petaniId,
            bulan = selectedBulan,
            tahun = selectedTahun,
            lahanId = selectedLahanId,
            tipe = selectedTipe,
            callback = object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this@RiwayatActivity,
                            e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val jsonString =
                        response.body?.string()

                    if(jsonString == null) return

                    val json =
                        JSONObject(jsonString)

                    val data =
                        json.getJSONArray("data")

                    val list =
                        mutableListOf<RiwayatItem>()

                    for(i in 0 until data.length()) {

                        val item =
                            data.getJSONObject(i)

                        list.add(

                            RiwayatItem(
                                id = item.getInt("id"),
                                judul = item.getString("judul"),
                                tanggal = item.getString("tanggal"),
                                nominal = item.getDouble("nominal"),
                                tipe = item.getString("tipe"),
                                lahanNama =
                                    item.optString(
                                        "lahan_nama"
                                    ),
                                sourceTable =
                                    item.getString(
                                        "source_table"
                                    )
                            )
                        )
                    }

                    runOnUiThread {

                        adapter.updateData(list)
                    }
                }
            }
        )
    }
    private fun showFilterPeriode() {

        val dialog = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.bottomsheet_filter_periode,
            null
        )

        dialog.setContentView(view)

        val spinnerBulan =
            view.findViewById<Spinner>(
                R.id.spinnerBulan
            )

        val spinnerTahun =
            view.findViewById<Spinner>(
                R.id.spinnerTahun
            )

        val btnTerapkan =
            view.findViewById<Button>(
                R.id.btnTerapkan
            )


        val bulanList = listOf(
            "Semua Bulan",
            "Januari",
            "Februari",
            "Maret",
            "April",
            "Mei",
            "Juni",
            "Juli",
            "Agustus",
            "September",
            "Oktober",
            "November",
            "Desember"
        )

        spinnerBulan.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                bulanList
            )

        val tahunList = mutableListOf(
            "Semua Tahun"
        )

        for(i in 2023..2035){
            tahunList.add(i.toString())
        }

        spinnerTahun.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                tahunList
            )
        val namaLahan =
            mutableListOf("Semua Lahan")

        namaLahan.addAll(
            lahanList.map {
                it.lahan_nama
            }
        )



        btnTerapkan.setOnClickListener {

            selectedBulan =
                if(spinnerBulan.selectedItemPosition == 0)
                    null
                else
                    spinnerBulan.selectedItemPosition

            selectedTahun =
                if(spinnerTahun.selectedItemPosition == 0)
                    null
                else
                    spinnerTahun.selectedItem.toString().toInt()


            updatePeriodeText()

            loadRiwayat()

            dialog.dismiss()
        }

        dialog.show()
    }
    private fun updatePeriodeText() {

        val namaBulan = arrayOf(
            "",
            "Januari",
            "Februari",
            "Maret",
            "April",
            "Mei",
            "Juni",
            "Juli",
            "Agustus",
            "September",
            "Oktober",
            "November",
            "Desember"
        )

        binding.tvPeriode.text = when {

            selectedBulan != null &&
                    selectedTahun != null ->

                "${namaBulan[selectedBulan!!]} $selectedTahun"

            selectedBulan != null ->

                namaBulan[selectedBulan!!]

            selectedTahun != null ->

                selectedTahun.toString()

            else ->

                "Semua Periode"
        }
    }


}