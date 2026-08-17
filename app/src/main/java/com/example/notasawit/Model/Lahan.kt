package com.example.notasawit.Model

import com.google.gson.annotations.SerializedName

data class Lahan(
    @SerializedName("lahan_id", alternate = ["id"])
    val lahan_id: Int,

    @SerializedName("lahan_nama", alternate = ["nama"])
    val lahan_nama: String,

    @SerializedName("lahan_luas", alternate = ["luas_lahan", "luas"])
    val lahan_luas: Double = 0.0
)