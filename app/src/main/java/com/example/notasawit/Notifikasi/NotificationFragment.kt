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
                    val idAudit = obj.getInt("id_audit")
                    val tanggal = obj.getString("tanggal")
                    val auditor = obj.getString("nama_auditor")
                    val isRead = if (obj.optBoolean("is_read", false) || obj.optInt("is_read", 0) == 1) 1 else 0
                    val pathFile = obj.optString("path_file_kunjungan", "")
                    
                    notificationList.add(NotificationItem(idAudit, tanggal, auditor, isRead, pathFile))
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
            PetaniApi.markNotificationAsRead(item.id_audit, object : Callback {
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

        // Buka file PDF jika ada
        val fileName = item.path_file_kunjungan
        if (!fileName.isNullOrEmpty() && fileName != "null") {
            // Asumsi file tersimpan di storage/audit_internal
            val url = "http://160.187.144.157/storage/$fileName"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "File PDF tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }
}
