package com.karenarias.proyectopmac.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL =
        "https://xiea4tw7p5.execute-api.us-east-1.amazonaws.com/"

    val recargaApi: RecargaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecargaApiService::class.java)
    }
}
