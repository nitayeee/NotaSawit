package com.example.notasawit.Autentikasi.Daftar.DataDiri

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.notasawit.R
import com.example.notasawit.databinding.FragmentDataDiri1Binding
import com.example.notasawit.databinding.FragmentDataDiri2Binding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DataDiri2Fragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DataDiri2Fragment : Fragment() {
    private var _binding: FragmentDataDiri2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDataDiri2Binding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnNext.setOnClickListener {
            // Validasi data dulu jika perlu
            (activity as? DataDiriActivity)?.nextStep()
        }
        super.onViewCreated(view, savedInstanceState)
    }


}