package com.example.locationhelper.network

import com.example.locationhelper.LocationEvent
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Response

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface NetworkApiService{


    @POST("saveLocation")
    suspend fun saveLocation(@Body requestBody: LocationEvent): Response<ApiResponse>

    @GET("saveErrorMsg")
    suspend fun logErrorMessages(@Query("errorMessage") message: String): Response<ApiResponse>

    }


   object NetworkHelper{

        fun getRetrofitInstance( baseUrl: String): NetworkApiService {

            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .baseUrl(baseUrl)
                .build()

            return retrofit.create(NetworkApiService::class.java)

        }

   }




