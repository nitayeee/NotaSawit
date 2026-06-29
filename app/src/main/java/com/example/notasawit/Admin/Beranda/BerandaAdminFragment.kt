package com.example.notasawit.Admin.Beranda

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.notasawit.R
import com.example.notasawit.databinding.FragmentBerandaAdminBinding
import com.example.notasawit.databinding.FragmentBerandaBinding


class BerandaAdminFragment : Fragment() {
    private var _binding: FragmentBerandaAdminBinding? = null
    private val binding get() = _binding!!

    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBerandaAdminBinding.inflate(inflater, container, false)
        return binding.root
    }


}