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
                    // Hapus sesi / shared preferences
                    val sharedPref = requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        clear()
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
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#1B4332"))
                dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#1B4332"))
            }
            
            dialog.show()
        }
    }
}
