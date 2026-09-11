package com.example.notasawit.Admin.Tugas

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.Admin.Beranda.Pengingat
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.databinding.ActivityDaftarTugasBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class DaftarTugasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarTugasBinding
    private lateinit var adapter: TugasAdapter
    private var allTugasList = mutableListOf<Pengingat>()
    private var currentFilter = "semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarTugasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        setupFilterChips()

        binding.swipeRefresh.setOnRefreshListener {
            fetchTugasData()
        }

        fetchTugasData()
    }

    private fun setupRecyclerView() {
        adapter = TugasAdapter(emptyList()) { task ->
            confirmCompleteTask(task)
        }
        binding.rvTugas.layoutManager = LinearLayoutManager(this)
        binding.rvTugas.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(binding.chipPending.id) -> "pending"
                checkedIds.contains(binding.chipSelesai.id) -> "selesai"
                else -> "semua"
            }
            applyFilter()
        }
    }

    private fun fetchTugasData() {
        binding.swipeRefresh.isRefreshing = true
        val sharedPref = getSharedPreferences("user_pref", MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null) ?: sharedPref.getString("id", null)

        PetaniApi.getDaftarTugas(userId, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@DaftarTugasActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                    applyFilter()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                val list = mutableListOf<Pengingat>()

                if (response.isSuccessful && !bodyStr.isNullOrEmpty()) {
                    try {
                        val jsonObj = JSONObject(bodyStr)
                        val dataArray = jsonObj.optJSONArray("data")
                        if (dataArray != null) {
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val isDone = item.optBoolean("is_done", false) || item.optInt("is_done", 0) == 1
                                list.add(
                                    Pengingat(
                                        id = item.optInt("id"),
                                        judul = item.optString("judul", "Tugas"),
                                        pesan = item.optString("pesan", ""),
                                        deadline = item.optString("deadline", ""),
                                        isDone = isDone
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                runOnUiThread {
                    binding.swipeRefresh.isRefreshing = false
                    allTugasList.clear()
                    allTugasList.addAll(list)
                    applyFilter()
                }
            }
        })
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "pending" -> allTugasList.filter { !it.isDone }
            "selesai" -> allTugasList.filter { it.isDone }
            else -> allTugasList
        }

        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            binding.rvTugas.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvTugas.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun confirmCompleteTask(task: Pengingat) {
        AlertDialog.Builder(this)
            .setTitle("Tandai Tugas Selesai")
            .setMessage("Apakah Anda yakin ingin menandai tugas \"${task.judul}\" sebagai selesai?")
            .setPositiveButton("Ya, Selesai") { dialog, _ ->
                dialog.dismiss()
                completeTask(task.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun completeTask(taskId: Int) {
        PetaniApi.completeTugas(taskId, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DaftarTugasActivity, "Gagal memperbarui status tugas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DaftarTugasActivity, "Tugas ditandai selesai!", Toast.LENGTH_SHORT).show()
                        fetchTugasData()
                    } else {
                        Toast.makeText(this@DaftarTugasActivity, "Gagal memproses tugas", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
