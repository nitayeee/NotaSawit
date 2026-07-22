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
    private val notificationList = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        rvNotifications = view.findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = NotificationAdapter(notificationList) { item ->
            handleNotificationClick(item)
        }
        rvNotifications.adapter = adapter

        fetchNotifications()

        return view
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

    private fun processResponse(response: Response) {
        if (!response.isSuccessful) return
        val body = response.body?.string() ?: return
        
        try {
            val jsonObject = JSONObject(body)
            val success = jsonObject.getBoolean("success")
            if (success) {
                val dataArray = jsonObject.getJSONArray("data")
                notificationList.clear()
                
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
                    
                    notificationList.add(NotificationItem(id, type, title, message, tanggal, isRead, dataUrl))
                }
                
                requireActivity().runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("NotificationFragment", "Gagal parsing JSON notifikasi", e)
        }
    }

    private fun handleNotificationClick(item: NotificationItem) {
        // Mark as read
        if (item.is_read == 0) {
            PetaniApi.markNotificationAsRead(item.type, item.id, object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        requireActivity().runOnUiThread {
                            item.is_read = 1
                            adapter.notifyDataSetChanged()
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
}
