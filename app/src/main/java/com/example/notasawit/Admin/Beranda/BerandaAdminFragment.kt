package com.example.notasawit.Admin.Beranda

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.databinding.FragmentBerandaAdminBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class BerandaAdminFragment : Fragment() {
    private var _binding: FragmentBerandaAdminBinding? = null
    private val binding get() = _binding!!

    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val username = sharedPref.getString("username", "Admin")
        binding.username.text = "$username!"
        if (!username.isNullOrEmpty()) {
            binding.tvInitial.text = username.substring(0, 1).uppercase()
        }

        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        PetaniApi.getDashboardData(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonObject = JSONObject(responseData)
                        if (jsonObject.getBoolean("success")) {
                            val dataObj = jsonObject.getJSONObject("data")
                            val jumlahPetani = dataObj.getInt("jumlah_petani")
                            val jumlahLahan = dataObj.getDouble("jumlah_lahan")
                            val pemasukanArray = dataObj.getJSONArray("pemasukan")
                            val pengeluaranArray = dataObj.getJSONArray("pengeluaran")

                            activity?.runOnUiThread {
                                binding.tvJumlahPetani.text = jumlahPetani.toString()
                                binding.tvLuasLahan.text = String.format("%.2f", jumlahLahan)
                                setupBarChart(pemasukanArray)
                                setupPieChart(pengeluaranArray)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun setupBarChart(pemasukanArray: org.json.JSONArray) {
        val entries = ArrayList<BarEntry>()
        for (i in 0 until pemasukanArray.length()) {
            entries.add(BarEntry(i.toFloat(), pemasukanArray.getInt(i).toFloat()))
        }

        val dataSet = BarDataSet(entries, "Pemasukan")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 10f

        val data = BarData(dataSet)
        data.barWidth = 0.5f

        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")

        binding.barChartPemasukan.apply {
            this.data = data
            description.isEnabled = false
            setFitBars(true)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(months)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
            }

            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false

            animateY(1000)
            invalidate()
        }
    }

    private fun setupPieChart(pengeluaranArray: org.json.JSONArray) {
        val entries = ArrayList<PieEntry>()
        for (i in 0 until pengeluaranArray.length()) {
            val item = pengeluaranArray.getJSONObject(i)
            entries.add(PieEntry(item.getInt("total").toFloat(), item.getString("jenis")))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)

        binding.pieChartPengeluaran.apply {
            this.data = data
            description.isEnabled = false
            centerText = "Pengeluaran"
            setEntryLabelColor(Color.BLACK)
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}