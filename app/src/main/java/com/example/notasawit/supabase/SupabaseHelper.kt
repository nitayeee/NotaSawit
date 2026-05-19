package com.example.notasawit.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth

object SupabaseHelper {
    // GANTI dengan URL dan ANON KEY dari Dashboard Supabase kamu!
    private const val SUPABASE_URL = "https://ewdvishjjenxdloozzjl.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImV3ZHZpc2hqamVueGRsb296empsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMDIyNzAsImV4cCI6MjA5Mzg3ODI3MH0.XUuJ7e36PCAvxU4fHhVQQr_UAzfi9Oo0Ni0B6W5PS2c"

    val client = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
        install(Auth)
    }
}