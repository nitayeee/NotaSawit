package com.example.notasawit.Admin.HargaTbs

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.Model.HargaTbsItem
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.databinding.ActivityRiwayatHargaTbsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

class RiwayatHargaTbsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatHargaTbsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiwayatHargaTbsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prevent overlap with status bar (battery/clock) and bottom navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }

        binding.rvRiwayatHargaTbs.layoutManager = LinearLayoutManager(this)

        loadData()
    }

    private fun parsePrice(json: JSONObject, key: String): Double {
        val str = json.optString(key, "0")
        return str.toDoubleOrNull() ?: json.optDouble(key, 0.0)
    }

    private fun loadData() {
        binding.pbLoading.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        PetaniApi.getRiwayatHargaTbs(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                fallbackToLatest()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val listData = mutableListOf<HargaTbsItem>()

                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    try {
                        val json = JSONObject(body)
                        if (json.optBoolean("success", false)) {
                            val array = json.optJSONArray("data")
                            if (array != null) {
                                for (i in 0 until array.length()) {
                                    val item = array.getJSONObject(i)
                                    val hDinas = parsePrice(item, "harga_dinas")
                                    val hPtSar = parsePrice(item, "harga_pt_sar")
                                    listData.add(
                                        HargaTbsItem(
                                            harga_tbs_id = item.optInt("harga_tbs_id", 0),
                                            harga_dinas = hDinas,
                                            harga_pt_sar = hPtSar,
                                            tanggal_berlaku = item.optString("tanggal_berlaku", "-"),
                                            created_at = item.optString("created_at", "")
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (listData.isEmpty()) {
                    fallbackToLatest()
                } else {
                    runOnUiThread {
                        binding.pbLoading.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false

                        binding.tvEmptyData.visibility = View.GONE
                        binding.rvRiwayatHargaTbs.visibility = View.VISIBLE
                        binding.tvCountBadge.text = "${listData.size} Data"

                        // Set Latest Header Card
                        val latest = listData[0]
                        val fmtDinas = NumberFormat.getNumberInstance(Locale("id", "ID")).format(latest.harga_dinas.toLong())
                        val fmtPtSar = NumberFormat.getNumberInstance(Locale("id", "ID")).format(latest.harga_pt_sar.toLong())

                        binding.tvTanggalTerbaru.text = "TMT: ${latest.tanggal_berlaku}"
                        binding.tvLatestDinas.text = "Rp $fmtDinas / Kg"
                        binding.tvLatestPtSar.text = "Rp $fmtPtSar / Kg"

                        // Set Adapter
                        binding.rvRiwayatHargaTbs.adapter = RiwayatHargaTbsAdapter(listData)

                        // Setup Chart (Chronological left-to-right)
                        setupChart(listData.reversed())
                    }
                }
            }
        })
    }

    private fun fallbackToLatest() {
        PetaniApi.getHargaTbs(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.pbLoading.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvEmptyData.visibility = View.VISIBLE
                    binding.rvRiwayatHargaTbs.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    binding.pbLoading.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && !body.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(body)
                            val data = if (json.has("data")) json.optJSONObject("data") else json
                            if (data != null) {
                                val hDinas = parsePrice(data, "harga_dinas")
                                val hPtSar = parsePrice(data, "harga_pt_sar")
                                val tgl = data.optString("tanggal_berlaku", data.optString("created_at", "Terbaru"))

                                val itemLatest = HargaTbsItem(
                                    harga_tbs_id = data.optInt("harga_tbs_id", 0),
                                    harga_dinas = hDinas,
                                    harga_pt_sar = hPtSar,
                                    tanggal_berlaku = tgl,
                                    created_at = data.optString("created_at", "")
                                )

                                val fmtDinas = NumberFormat.getNumberInstance(Locale("id", "ID")).format(hDinas.toLong())
                                val fmtPtSar = NumberFormat.getNumberInstance(Locale("id", "ID")).format(hPtSar.toLong())

                                binding.tvTanggalTerbaru.text = "TMT: $tgl"
                                binding.tvLatestDinas.text = "Rp $fmtDinas / Kg"
                                binding.tvLatestPtSar.text = "Rp $fmtPtSar / Kg"

                                binding.tvEmptyData.visibility = View.GONE
                                binding.rvRiwayatHargaTbs.visibility = View.VISIBLE
                                binding.tvCountBadge.text = "1 Data"
                                binding.rvRiwayatHargaTbs.adapter = RiwayatHargaTbsAdapter(listOf(itemLatest))
                                setupChart(listOf(itemLatest))
                                return@runOnUiThread
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    binding.tvEmptyData.visibility = View.VISIBLE
                    binding.rvRiwayatHargaTbs.visibility = View.GONE
                }
            }
        })
    }

    private fun setupChart(chronologicalList: List<HargaTbsItem>) {
        if (chronologicalList.isEmpty()) return

        val dinasEntries = ArrayList<Entry>()
        val ptSarEntries = ArrayList<Entry>()
        val dates = ArrayList<String>()

        for (i in chronologicalList.indices) {
            val item = chronologicalList[i]
            dinasEntries.add(Entry(i.toFloat(), item.harga_dinas.toFloat()))
            ptSarEntries.add(Entry(i.toFloat(), item.harga_pt_sar.toFloat()))

            val parts = item.tanggal_berlaku.split("-")
            val dateLabel = if (parts.size >= 3) "${parts[2]}/${parts[1]}" else item.tanggal_berlaku
            dates.add(dateLabel)
        }

        val dataSetDinas = LineDataSet(dinasEntries, "Dinas").apply {
            color = Color.parseColor("#10B981")
            setCircleColor(Color.parseColor("#10B981"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9.5f
            valueTextColor = Color.parseColor("#1E293B")
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val dataSetPtSar = LineDataSet(ptSarEntries, "PT. SAR").apply {
            color = Color.parseColor("#D4AF37")
            setCircleColor(Color.parseColor("#D4AF37"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9.5f
            valueTextColor = Color.parseColor("#1E293B")
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(dataSetDinas, dataSetPtSar)

        binding.lineChartHargaTbs.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(dates)
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.parseColor("#64748B")
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F1F5F9")
                textColor = Color.parseColor("#64748B")
            }

            axisRight.isEnabled = false

            animateX(1000)
            invalidate()
        }
    }
}
