package com.example.notasawit.Notifikasi

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class NotificationFragment : Fragment() {

    private lateinit var rvNotifications: RecyclerView
    private val allNotifications = mutableListOf<NotificationItem>()
    private val notificationList = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter
    
    private lateinit var filterSemua: android.widget.TextView
    private lateinit var filterBelum: android.widget.TextView
    private lateinit var filterSudah: android.widget.TextView
    
    private lateinit var panelSelection: android.widget.LinearLayout
    private lateinit var btnBatalSeleksi: android.widget.Button
    private lateinit var btnTandaiDibaca: android.widget.Button
    
    private var currentFilter = "semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        rvNotifications = view.findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        
        filterSemua = view.findViewById(R.id.filterSemua)
        filterBelum = view.findViewById(R.id.filterBelum)
        filterSudah = view.findViewById(R.id.filterSudah)
        
        panelSelection = view.findViewById(R.id.panelSelection)
        btnBatalSeleksi = view.findViewById(R.id.btnBatalSeleksi)
        btnTandaiDibaca = view.findViewById(R.id.btnTandaiDibaca)
        
        adapter = NotificationAdapter(
            notificationList,
            onItemClick = { item ->
                handleNotificationClick(item)
            },
            onItemLongClick = { item ->
                handleNotificationLongClick(item)
            }
        )
        rvNotifications.adapter = adapter

        setupFilterListeners()
        setupSelectionListeners()
        fetchNotifications()

        return view
    }

    private fun setupSelectionListeners() {
        btnBatalSeleksi.setOnClickListener {
            adapter.isSelectionMode = false
            allNotifications.forEach { it.isSelected = false }
            panelSelection.visibility = View.GONE
            applyFilter()
        }

        btnTandaiDibaca.setOnClickListener {
            val selectedItems = allNotifications.filter { it.isSelected && it.is_read == 0 }
            if (selectedItems.isEmpty()) {
                adapter.isSelectionMode = false
                panelSelection.visibility = View.GONE
                applyFilter()
                return@setOnClickListener
            }

            var pendingCount = selectedItems.size
            selectedItems.forEach { item ->
                PetaniApi.markNotificationAsRead(item.type, item.id, object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        checkPendingCount(--pendingCount)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            item.is_read = 1
                        }
                        checkPendingCount(--pendingCount)
                    }
                })
            }
        }
    }

    private fun checkPendingCount(count: Int) {
        if (count == 0) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Berhasil menandai notifikasi", Toast.LENGTH_SHORT).show()
                adapter.isSelectionMode = false
                allNotifications.forEach { it.isSelected = false }
                panelSelection.visibility = View.GONE
                applyFilter()
            }
        }
    }

    private fun fetchNotifications() {
        val sharedPref = requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        var petaniId = sharedPref.getInt("petani_id", -1)
        
        if (petaniId == -1 || petaniId == 0) {
            // Coba ambil dari user_id jika petani_id kosong
            petaniId = sharedPref.getInt("user_id", -1)
        }

        if (petaniId == -1 || petaniId == 0) {
            Log.e("NotificationFragment", "ID Petani tidak ditemukan di SharedPreferences")
            return
        }

        PetaniApi.getNotifications(petaniId, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NotificationFragment", "Gagal mengambil notifikasi: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) { 
                processResponse(response) 
            }
        })
    }

    private fun setupFilterListeners() {
        filterSemua.setOnClickListener {
            currentFilter = "semua"
            updateFilterUI()
            applyFilter()
        }
        filterBelum.setOnClickListener {
            currentFilter = "belum"
            updateFilterUI()
            applyFilter()
        }
        filterSudah.setOnClickListener {
            currentFilter = "sudah"
            updateFilterUI()
            applyFilter()
        }
    }

    private fun updateFilterUI() {
        val activeBg = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2B462C"))
        val inactiveBg = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8EEE8"))
        val activeText = android.graphics.Color.parseColor("#FFFFFF")
        val inactiveText = android.graphics.Color.parseColor("#2B462C")

        filterSemua.backgroundTintList = if (currentFilter == "semua") activeBg else inactiveBg
        filterSemua.setTextColor(if (currentFilter == "semua") activeText else inactiveText)

        filterBelum.backgroundTintList = if (currentFilter == "belum") activeBg else inactiveBg
        filterBelum.setTextColor(if (currentFilter == "belum") activeText else inactiveText)

        filterSudah.backgroundTintList = if (currentFilter == "sudah") activeBg else inactiveBg
        filterSudah.setTextColor(if (currentFilter == "sudah") activeText else inactiveText)
    }

    private fun applyFilter() {
        notificationList.clear()
        when (currentFilter) {
            "semua" -> notificationList.addAll(allNotifications)
            "belum" -> notificationList.addAll(allNotifications.filter { it.is_read == 0 })
            "sudah" -> notificationList.addAll(allNotifications.filter { it.is_read == 1 })
        }
        adapter.notifyDataSetChanged()
    }

    private fun processResponse(response: Response) {
        if (!response.isSuccessful) return
        val body = response.body?.string() ?: return
        
        try {
            val jsonObject = JSONObject(body)
            val success = jsonObject.getBoolean("success")
            if (success) {
                val dataArray = jsonObject.getJSONArray("data")
                allNotifications.clear()
                
                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val id = obj.optInt("id", 0)
                    if (id == 0) {
                        Log.e("NotificationFragment", "ID kosong atau null pada JSON: $obj")
                        continue
                    }
                    val type = obj.getString("type")
                    val title = obj.getString("title")
                    val message = obj.getString("message")
                    val tanggal = obj.getString("tanggal")
                    val isRead = if (obj.optBoolean("is_read", false) || obj.optInt("is_read", 0) == 1) 1 else 0
                    val dataUrl = obj.optString("data_url", "")
                    
                    allNotifications.add(NotificationItem(id, type, title, message, tanggal, isRead, dataUrl))
                }
                
                requireActivity().runOnUiThread {
                    applyFilter()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("NotificationFragment", "Gagal parsing JSON notifikasi", e)
        }
    }

    private fun handleNotificationClick(item: NotificationItem) {
        if (adapter.isSelectionMode) {
            item.isSelected = !item.isSelected
            adapter.notifyDataSetChanged()
            updateSelectionCount()
            return
        }

        // Mark as read
        if (item.is_read == 0) {
            PetaniApi.markNotificationAsRead(item.type, item.id, object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        requireActivity().runOnUiThread {
                            item.is_read = 1
                            val index = allNotifications.indexOfFirst { it.id == item.id && it.type == item.type }
                            if (index != -1) {
                                allNotifications[index].is_read = 1
                            }
                            applyFilter()
                        }
                    }
                }
            })
        }

        // Navigate or open based on type
        when (item.type) {
            "audit" -> {
                val fileName = item.data_url
                if (!fileName.isNullOrEmpty() && fileName != "null") {
                    val url = "http://160.187.144.157/storage/$fileName"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "File PDF tidak tersedia", Toast.LENGTH_SHORT).show()
                }
            }
            "produksi" -> {
                val intent = Intent(requireContext(), com.example.notasawit.DetailRiwayatKeuangan.RiwayatPemasukanActivity::class.java)
                intent.putExtra("produksi_id", item.id)
                startActivity(intent)
            }
            "pengeluaran" -> {
                val intent = Intent(requireContext(), com.example.notasawit.DetailRiwayatKeuangan.RiwayatPengeluaranActivity::class.java)
                intent.putExtra("biaya_id", item.id)
                startActivity(intent)
            }
        }
    }

    private fun handleNotificationLongClick(item: NotificationItem) {
        if (!adapter.isSelectionMode) {
            adapter.isSelectionMode = true
            item.isSelected = true
            panelSelection.visibility = View.VISIBLE
            updateSelectionCount()
            applyFilter()
        }
    }

    private fun updateSelectionCount() {
        val selectedCount = allNotifications.count { it.isSelected }
        btnTandaiDibaca.text = "Tandai Dibaca ($selectedCount)"
        if (selectedCount == 0) {
            adapter.isSelectionMode = false
            panelSelection.visibility = View.GONE
            applyFilter()
        }
    }
}
