package com.example.notasawit.Autentikasi.Daftar.DataDiri

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.R
import com.example.notasawit.databinding.FragmentDataDiri3Binding
import com.example.notasawit.Network.PetaniApi
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest

class DataDiri3Fragment : Fragment() {

    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    private var _binding: FragmentDataDiri3Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataDiri3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPinInputs()

        binding.btnVerify.setOnClickListener {

            val pinMentah = ambilPinInput()

            if (pinMentah.length != 6) {
                Toast.makeText(requireContext(), "PIN harus 6 digit!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pinHash = hashPin(pinMentah)

            val nama = sharedPref.getString("namaPetani", "") ?: ""
            val username = sharedPref.getString("username", "") ?: ""
            val tglLahir = sharedPref.getString("tanggalLahir", "") ?: ""
            val jk = sharedPref.getString("jenisKelamin", "") ?: ""
            val email = sharedPref.getString("emailPetani", "") ?: ""
            val noHp = sharedPref.getString("noHpPetani", "") ?: ""
            val alamat = sharedPref.getString("alamatPetani", "") ?: ""
            val desa = sharedPref.getInt("desaPetani", 0)

            PetaniApi.registerPetani(
                nama,
                username,
                tglLahir,
                jk,
                email,
                noHp,
                alamat,
                desa,
                pinHash,
                object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        activity?.runOnUiThread {
                            Log.e("REGISTER", "Request gagal", e)
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: ""

                        Log.d("REGISTER", "Code: ${response.code}")
                        Log.d("REGISTER", "Response: $responseBody")

                        activity?.runOnUiThread {
                            if (response.isSuccessful) {

                                Toast.makeText(
                                    requireContext(),
                                    "Pendaftaran Berhasil!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                tampilkanDialogCustom()

                            } else {

                                Toast.makeText(
                                    requireContext(),
                                    "Gagal daftar (${response.code})",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.e(
                                    "REGISTER",
                                    "Error ${response.code}: $responseBody"
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    private fun setupPinInputs() {
        val pins = arrayOf(
            binding.pin1, binding.pin2, binding.pin3,
            binding.pin4, binding.pin5, binding.pin6
        )

        for (i in pins.indices) {
            pins[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < pins.size - 1) {
                        pins[i + 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 0 && i > 0) {
                        pins[i - 1].requestFocus()
                    }
                }
            })
        }
    }

    private fun ambilPinInput(): String {
        return binding.pin1.text.toString() +
                binding.pin2.text.toString() +
                binding.pin3.text.toString() +
                binding.pin4.text.toString() +
                binding.pin5.text.toString() +
                binding.pin6.text.toString()
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun tampilkanDialogCustom() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_sukses_daftar)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnOk = dialog.findViewById<Button>(R.id.btnOk)

        btnOk.setOnClickListener {
            dialog.dismiss()

            val intent = Intent(
                requireContext(),
                com.example.notasawit.Autentikasi.Masuk.MasukActivity::class.java
            )

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}