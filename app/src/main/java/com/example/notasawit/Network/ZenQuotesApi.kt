package com.example.notasawit.Network



import com.example.notasawit.Model.QuoteResponse
import retrofit2.Call
import retrofit2.http.GET

interface ZenQuotesApi {

    @GET("api/random")
    fun getRandomQuote(): Call<List<QuoteResponse>>
}