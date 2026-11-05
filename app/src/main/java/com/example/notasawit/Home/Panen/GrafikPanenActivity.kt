package com.example.notasawit.Home.Panen

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.Network.ApiClient
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.ActivityGrafikPanenBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GrafikPanenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGrafikPanenBinding
    private lateinit var adapter: PanenBulananAdapter

    private var petaniId: Int = 0
    private var selectedYear: Int = 2026
    private var selectedLahanId: Int = -1 // -1 = Semua Lahan

    private data class RawProduksiRecord(
        val tanggal: String,
        val jumlahKg: Double,
        val nominalRp: Double,
        val lahanId: Int?,
        val lahanNama: String?
    )

    private val allProduksiList = mutableListOf<RawProduksiRecord>()
    private val availableYears = mutableListOf<Int>()
    private val lahanList = mutableListOf<Pair<Int, String>>() // Pair(id, nama)

    private val monthNames = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    private val monthAbbrs = arrayOf(
        "JAN", "FEB", "MAR", "APR", "MEI", "JUN",
        "JUL", "AGT", "SEP", "OKT", "NOV", "DES"
    )

    private val df = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale("id", "ID")))
    private val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGrafikPanenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        petaniId = sharedPref.getInt("petani_id", 0)
        if (petaniId == 0) {
            petaniId = sharedPref.getInt("user_id", 0)
        }

        val cal = Calendar.getInstance()
        selectedYear = cal.get(Calendar.YEAR)

        setupUI()
        setupRecyclerView()
        setupListeners()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // Inisialisasi daftar tahun (menyediakan tahun sekarang, tahun depan, dan tahun-tahun sebelumnya)
        populateAvailableYears()

        // Inisialisasi daftar lahan
        lahanList.clear()
        lahanList.add(Pair(-1, "Semua Lahan"))
        updateLahanSpinner()

        // Inisialisasi tampilan grafik awal
        setupEmptyCharts()
    }

    private fun setupRecyclerView() {
        adapter = PanenBulananAdapter { item ->
            // Saat item list diklik, sorot bar / node di grafik
            val index = item.monthIndex - 1
            if (index in 0..11) {
                if (binding.barChartPanen.visibility == View.VISIBLE) {
                    binding.barChartPanen.highlightValue(index.toFloat(), 0)
                } else {
                    binding.lineChartPanen.highlightValue(index.toFloat(), 0)
                }
                showSelectedMonthDetail(item)
            }
        }
        binding.rvPanenBulanan.layoutManager = LinearLayoutManager(this)
        binding.rvPanenBulanan.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }

        // Toggle Tipe Grafik: Batang vs Garis
        binding.toggleChartType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnChartBar) {
                    binding.barChartPanen.visibility = View.VISIBLE
                    binding.lineChartPanen.visibility = View.GONE
                    binding.barChartPanen.animateY(600)
                } else {
                    binding.barChartPanen.visibility = View.GONE
                    binding.lineChartPanen.visibility = View.VISIBLE
                    binding.lineChartPanen.animateX(600)
                }
            }
        }

        // Spinner Tahun
        binding.spinnerTahun.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in availableYears.indices) {
                    val year = availableYears[position]
                    if (year != selectedYear) {
                        selectedYear = year
                        binding.tvToolbarSubtitle.text = "Tren Produksi TBS Tahun $selectedYear"
                        processAndDisplayData()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Spinner Lahan
        binding.spinnerLahan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in lahanList.indices) {
                    val lahan = lahanList[position]
                    if (lahan.first != selectedLahanId) {
                        selectedLahanId = lahan.first
                        processAndDisplayData()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun populateAvailableYears(yearsFound: List<Int> = emptyList()) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = 2020 // Sediakan pilihan tahun ke belakang hingga 2020
        val maxYear = currentYear + 1 // dan tahun depan

        val yearsSet = sortedSetOf<Int>(Comparator.reverseOrder())
        for (y in maxYear downTo minYear) {
            yearsSet.add(y)
        }
        yearsSet.addAll(yearsFound)

        availableYears.clear()
        availableYears.addAll(yearsSet)
        updateYearSpinner()
    }

    private fun updateYearSpinner() {
        val adapterTahun = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            availableYears.map { "Tahun $it" }
        )
        adapterTahun.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTahun.adapter = adapterTahun

        val index = availableYears.indexOf(selectedYear)
        if (index >= 0) {
            binding.spinnerTahun.setSelection(index)
        }
    }

    private fun updateLahanSpinner() {
        val adapterLahan = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            lahanList.map { it.second }
        )
        adapterLahan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLahan.adapter = adapterLahan
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@GrafikPanenActivity)

            // 1. Ambil daftar lahan dari Room untuk filter lahan
            val localLahan = try {
                db.LahanDao().getLahanByPetani(petaniId)
            } catch (e: Exception) {
                emptyList()
            }

            // 2. Ambil data lokal Room yang belum disinkron
            val localUnsynced = try {
                db.ProduksiDao().getAll()
            } catch (e: Exception) {
                emptyList()
            }

            val localRecords = localUnsynced.map {
                RawProduksiRecord(
                    tanggal = it.produksi_tanggal,
                    jumlahKg = it.jumlah_tbs.toDouble(),
                    nominalRp = it.total_pendapatan,
                    lahanId = null,
                    lahanNama = null
                )
            }

            withContext(Dispatchers.Main) {
                if (localLahan.isNotEmpty()) {
                    lahanList.clear()
                    lahanList.add(Pair(-1, "Semua Lahan"))
                    localLahan.forEach { l ->
                        lahanList.add(Pair(l.lahan_id, l.lahan_nama))
                    }
                    updateLahanSpinner()
                }
            }

            // 3. Ambil data riwayat keuangan lengkap dari server
            PetaniApi.getRiwayatKeuangan(
                petaniId = petaniId,
                bulan = null,
                tahun = null,
                lahanId = null,
                tipe = "pemasukan",
                callback = object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.swipeRefresh.isRefreshing = false
                            allProduksiList.clear()
                            allProduksiList.addAll(localRecords)
                            processAndDisplayData()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val jsonString = response.body?.string() ?: ""
                        val serverRecords = mutableListOf<RawProduksiRecord>()

                        try {
                            val json = JSONObject(jsonString)
                            val data = json.optJSONArray("data")
                            if (data != null) {
                                for (i in 0 until data.length()) {
                                    val item = data.optJSONObject(i) ?: continue
                                    val tanggal = item.optString("tanggal", "")
                                    val nominal = try {
                                        if (item.has("nominal") && !item.isNull("nominal")) {
                                            item.optString("nominal").toDoubleOrNull() ?: item.optDouble("nominal", 0.0)
                                        } else 0.0
                                    } catch (e: Exception) { 0.0 }

                                    val jumlahKg = try {
                                        when {
                                            item.has("jumlah_tbs") && !item.isNull("jumlah_tbs") -> {
                                                item.optString("jumlah_tbs").toDoubleOrNull() ?: item.optDouble("jumlah_tbs", 0.0)
                                            }
                                            item.has("jumlah") && !item.isNull("jumlah") -> {
                                                item.optString("jumlah").toDoubleOrNull() ?: item.optDouble("jumlah", 0.0)
                                            }
                                            else -> 0.0
                                        }
                                    } catch (e: Exception) { 0.0 }

                                    val lahanIdVal = if (item.has("lahan_id") && !item.isNull("lahan_id")) item.optInt("lahan_id") else null
                                    val lahanNamaVal = if (item.isNull("lahan_nama")) null else item.optString("lahan_nama")

                                    if (tanggal.isNotEmpty()) {
                                        serverRecords.add(
                                            RawProduksiRecord(
                                                tanggal = tanggal,
                                                jumlahKg = jumlahKg,
                                                nominalRp = nominal,
                                                lahanId = lahanIdVal,
                                                lahanNama = lahanNamaVal
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("GRAFIK_PANEN", "Error parsing riwayat: ${e.message}")
                        }

                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.swipeRefresh.isRefreshing = false
                            allProduksiList.clear()
                            allProduksiList.addAll(serverRecords)
                            allProduksiList.addAll(localRecords)

                            // Kumpulkan semua tahun unik yang terdapat di data dan perbarui spinner tahun
                            val yearsFound = allProduksiList.mapNotNull {
                                val parts = it.tanggal.split("-")
                                if (parts.isNotEmpty()) parts[0].toIntOrNull() else null
                            }.distinct()

                            populateAvailableYears(yearsFound)

                            processAndDisplayData()
                        }
                    }
                }
            )
        }
    }

    private fun processAndDisplayData() {
        val yearPrefix = "$selectedYear-"

        // Filter data berdasarkan tahun dan lahan yang dipilih
        val filteredList = allProduksiList.filter { record ->
            val matchesYear = record.tanggal.startsWith(yearPrefix)
            val matchesLahan = if (selectedLahanId == -1) {
                true
            } else {
                record.lahanId == selectedLahanId
            }
            matchesYear && matchesLahan
        }

        // Kelompokkan 12 bulan (indeks 1 s.d. 12)
        val monthlyKg = DoubleArray(12) { 0.0 }
        val monthlyRp = DoubleArray(12) { 0.0 }
        val monthlyCount = IntArray(12) { 0 }

        filteredList.forEach { record ->
            val parts = record.tanggal.split("-")
            if (parts.size >= 2) {
                val monthIndex = parts[1].toIntOrNull() // 1 - 12
                if (monthIndex != null && monthIndex in 1..12) {
                    val idx = monthIndex - 1
                    monthlyKg[idx] += record.jumlahKg
                    monthlyRp[idx] += record.nominalRp
                    monthlyCount[idx] += 1
                }
            }
        }

        // Hitung Ringkasan KPI
        val totalPanenKg = monthlyKg.sum()
        val totalPanenTon = totalPanenKg / 1000.0

        // Format KPI 1: Total Panen
        if (totalPanenKg >= 1000.0) {
            binding.tvKpiTotalPanen.text = "${df.format(totalPanenTon)} Ton"
            binding.tvKpiTotalKg.text = "${df.format(totalPanenKg)} Kg"
        } else {
            binding.tvKpiTotalPanen.text = "${df.format(totalPanenKg)} Kg"
            binding.tvKpiTotalKg.text = "${String.format(Locale("id", "ID"), "%.2f", totalPanenTon)} Ton"
        }

        // Format KPI 2: Rata-Rata Bulanan
        val activeMonthsCount = monthlyCount.count { it > 0 }
        val divisor = if (activeMonthsCount > 0) activeMonthsCount else 12
        val rataRataKg = totalPanenKg / divisor
        if (rataRataKg >= 1000.0) {
            binding.tvKpiRataRata.text = "${df.format(rataRataKg / 1000.0)} Ton"
        } else {
            binding.tvKpiRataRata.text = "${df.format(rataRataKg)} Kg"
        }

        // Format KPI 3: Bulan Tertinggi
        var maxMonthIdx = -1
        var maxMonthKg = 0.0
        for (i in 0..11) {
            if (monthlyKg[i] > maxMonthKg) {
                maxMonthKg = monthlyKg[i]
                maxMonthIdx = i
            }
        }

        if (maxMonthIdx != -1 && maxMonthKg > 0) {
            binding.tvKpiTertinggiBulan.text = monthAbbrs[maxMonthIdx]
            val maxTon = maxMonthKg / 1000.0
            if (maxMonthKg >= 1000.0) {
                binding.tvKpiTertinggiNilai.text = "${df.format(maxTon)} Ton"
            } else {
                binding.tvKpiTertinggiNilai.text = "${df.format(maxMonthKg)} Kg"
            }
        } else {
            binding.tvKpiTertinggiBulan.text = "-"
            binding.tvKpiTertinggiNilai.text = "0 Ton"
        }

        // Setup Grafik Bar & Line
        renderCharts(monthlyKg, monthlyRp, monthlyCount)

        // Susun daftar item untuk RecyclerView (urutkan dari bulan terbaru: Desember ke Januari)
        val yearAbbrStr = "'${selectedYear.toString().takeLast(2)}"
        val listItems = mutableListOf<PanenBulanItem>()

        for (i in 11 downTo 0) {
            val prevKg = if (i > 0) monthlyKg[i - 1] else 0.0
            val curKg = monthlyKg[i]
            val pctChange = if (prevKg > 0) {
                ((curKg - prevKg) / prevKg) * 100.0
            } else null

            listItems.add(
                PanenBulanItem(
                    monthIndex = i + 1,
                    year = selectedYear,
                    monthName = monthNames[i],
                    monthAbbr = monthAbbrs[i],
                    yearAbbr = yearAbbrStr,
                    totalKg = curKg,
                    totalRp = monthlyRp[i],
                    transactionCount = monthlyCount[i],
                    percentageFromPrev = pctChange
                )
            )
        }

        adapter.submitList(listItems)

        // Update badge dan empty state
        val totalTransaksiTahunIni = monthlyCount.sum()
        if (totalTransaksiTahunIni > 0) {
            binding.rvPanenBulanan.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            binding.tvBulanCountBadge.text = "$totalTransaksiTahunIni Transaksi"
        } else {
            binding.rvPanenBulanan.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            binding.tvBulanCountBadge.text = "0 Transaksi"
        }
    }

    private fun setupEmptyCharts() {
        val months = monthAbbrs

        // Setup BarChart
        binding.barChartPanen.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(true)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(months)
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
                textColor = Color.parseColor("#64748B")
                textSize = 10f
            }

            axisLeft.apply {
                axisMinimum = 0f
                textColor = Color.parseColor("#64748B")
                textSize = 10f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F1F5F9")
            }

            axisRight.isEnabled = false
        }

        // Setup LineChart
        binding.lineChartPanen.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(months)
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
                textColor = Color.parseColor("#64748B")
                textSize = 10f
            }

            axisLeft.apply {
                axisMinimum = 0f
                textColor = Color.parseColor("#64748B")
                textSize = 10f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F1F5F9")
            }

            axisRight.isEnabled = false
        }
    }

    private fun renderCharts(monthlyKg: DoubleArray, monthlyRp: DoubleArray, monthlyCount: IntArray) {
        val barEntries = ArrayList<BarEntry>()
        val lineEntries = ArrayList<Entry>()

        for (i in 0..11) {
            val valKg = monthlyKg[i].toFloat()
            barEntries.add(BarEntry(i.toFloat(), valKg))
            lineEntries.add(Entry(i.toFloat(), valKg))
        }

        // 1. Data Set Bar
        val barDataSet = BarDataSet(barEntries, "Produksi (Kg)").apply {
            color = Color.parseColor("#1B4D2E")
            highLightColor = Color.parseColor("#D4AF37")
            setDrawValues(true)
            valueTextSize = 9f
            valueTextColor = Color.parseColor("#1E293B")
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value <= 0f) return ""
                    return if (value >= 1000f) {
                        "${df.format(value / 1000f)}T"
                    } else {
                        "${df.format(value.toDouble())}k"
                    }
                }
            }
        }

        val barData = BarData(barDataSet).apply {
            barWidth = 0.6f
        }

        binding.barChartPanen.apply {
            data = barData
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val idx = e?.x?.toInt() ?: return
                    if (idx in 0..11) {
                        val item = PanenBulanItem(
                            monthIndex = idx + 1,
                            year = selectedYear,
                            monthName = monthNames[idx],
                            monthAbbr = monthAbbrs[idx],
                            yearAbbr = "'${selectedYear.toString().takeLast(2)}",
                            totalKg = monthlyKg[idx],
                            totalRp = monthlyRp[idx],
                            transactionCount = monthlyCount[idx]
                        )
                        showSelectedMonthDetail(item)
                    }
                }

                override fun onNothingSelected() {
                    binding.cardSelectedMonthDetail.visibility = View.GONE
                }
            })
            animateY(700)
            invalidate()
        }

        // 2. Data Set Line
        val lineDataSet = LineDataSet(lineEntries, "Produksi (Kg)").apply {
            color = Color.parseColor("#1B4D2E")
            lineWidth = 2.8f
            setCircleColor(Color.parseColor("#D4AF37"))
            circleHoleColor = Color.WHITE
            circleRadius = 4.5f
            setDrawCircleHole(true)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#1B4D2E")
            fillAlpha = 35
            setDrawValues(true)
            valueTextSize = 9f
            valueTextColor = Color.parseColor("#1E293B")
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value <= 0f) return ""
                    return if (value >= 1000f) {
                        "${df.format(value / 1000f)}T"
                    } else {
                        "${df.format(value.toDouble())}k"
                    }
                }
            }
        }

        val lineData = LineData(lineDataSet)

        binding.lineChartPanen.apply {
            data = lineData
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val idx = e?.x?.toInt() ?: return
                    if (idx in 0..11) {
                        val item = PanenBulanItem(
                            monthIndex = idx + 1,
                            year = selectedYear,
                            monthName = monthNames[idx],
                            monthAbbr = monthAbbrs[idx],
                            yearAbbr = "'${selectedYear.toString().takeLast(2)}",
                            totalKg = monthlyKg[idx],
                            totalRp = monthlyRp[idx],
                            transactionCount = monthlyCount[idx]
                        )
                        showSelectedMonthDetail(item)
                    }
                }

                override fun onNothingSelected() {
                    binding.cardSelectedMonthDetail.visibility = View.GONE
                }
            })
            animateX(700)
            invalidate()
        }
    }

    private fun showSelectedMonthDetail(item: PanenBulanItem) {
        binding.cardSelectedMonthDetail.visibility = View.VISIBLE
        binding.tvDetailBulanNama.text = "${item.monthName} ${item.year}"
        binding.tvDetailTransaksiCount.text = "${item.transactionCount}x panen"

        if (item.totalKg >= 1000.0) {
            val tonVal = item.totalKg / 1000.0
            binding.tvDetailPanenKg.text = "${df.format(tonVal)} Ton (${df.format(item.totalKg)} Kg)"
        } else {
            binding.tvDetailPanenKg.text = "${df.format(item.totalKg)} Kg"
        }

        val rpFormatted = currencyFormat.format(item.totalRp).replace("Rp", "Rp ")
        binding.tvDetailPendapatanRp.text = rpFormatted
    }
}

