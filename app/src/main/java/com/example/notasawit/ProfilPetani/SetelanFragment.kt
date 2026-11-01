package com.example.notasawit.ProfilPetani

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.notasawit.Autentikasi.Masuk.MasukActivity
import com.example.notasawit.R
import androidx.fragment.app.Fragment

class SetelanFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setelan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnUbahPassword = view.findViewById<View>(R.id.btnUbahPassword)
        val btnTentang = view.findViewById<View>(R.id.btnTentang)
        val btnBantuan = view.findViewById<View>(R.id.btnBantuan)
        val btnLogout = view.findViewById<View>(R.id.btnLogout)

        btnUbahPassword.setOnClickListener {
            val intent = Intent(requireContext(), UbahPinActivity::class.java)
            startActivity(intent)
        }

        btnTentang.setOnClickListener {
            val intent = Intent(requireContext(), TentangAplikasiActivity::class.java)
            startActivity(intent)
        }

        btnBantuan.setOnClickListener {
            val intent = Intent(requireContext(), BantuanActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("OK") { dialogInterface, which ->
                    // Ambil data username & remember me sebelum menghapus sesi login
                    val sharedPref = requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
                    val lastUsername = sharedPref.getString("last_username", "")?.ifEmpty { null }
                        ?: sharedPref.getString("username", "") ?: ""
                    val isRemembered = sharedPref.getBoolean("remember_me", true)

                    with(sharedPref.edit()) {
                        clear()
                        putBoolean("is_logged_in", false)
                        putBoolean("is_first_time", false)
                        // Pertahankan username agar petani tidak perlu mengetik ulang saat login kembali
                        if (isRemembered && lastUsername.isNotEmpty()) {
                            putString("last_username", lastUsername)
                            putBoolean("remember_me", true)
                        }
                        apply()
                    }
                    
                    // Pindah ke halaman Masuk
                    val intent = Intent(requireContext(), MasukActivity::class.java)
                    // Hapus backstack agar tidak bisa kembali dengan tombol back
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Batal") { dialogInterface, which ->
                    dialogInterface.dismiss()
                }
                .create()
                
            dialog.setOnShowListener {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#1B4D2E"))
                dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#757575"))
            }
            dialog.show()
        }
    }
}
