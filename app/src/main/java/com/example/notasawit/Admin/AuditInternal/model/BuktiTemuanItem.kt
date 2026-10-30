package com.example.notasawit.Admin.AuditInternal.model

import org.json.JSONArray
import org.json.JSONObject

data class BuktiTemuanItem(
    val fotoPath: String,
    var keterangan: String = ""
)

object BuktiTemuanHelper {
    fun toJson(list: List<BuktiTemuanItem>): String {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("fotoPath", item.fotoPath)
                put("keterangan", item.keterangan)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(jsonStr: String): MutableList<BuktiTemuanItem> {
        val result = mutableListOf<BuktiTemuanItem>()
        if (jsonStr.isBlank()) return result
        try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val path = obj.optString("fotoPath", "")
                    val ket = obj.optString("keterangan", "")
                    if (path.isNotEmpty()) {
                        result.add(BuktiTemuanItem(fotoPath = path, keterangan = ket))
                    }
                }
            } else if (trimmed.isNotEmpty()) {
                // Backwards-compatibility jika fotoPath sebelumnya hanya path tunggal
                result.add(BuktiTemuanItem(fotoPath = trimmed, keterangan = ""))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (jsonStr.isNotBlank()) {
                result.add(BuktiTemuanItem(fotoPath = jsonStr, keterangan = ""))
            }
        }
        return result
    }
}
