package com.democalling

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("users")
    fun getUsers(): Call<List<User>> // Adjust endpoint accordingly

    // Using @Path to dynamically insert the ID into the URL
    @GET("posts/{id}")
    fun getPostById(@Path("id") postId: Int): Call<Post> // Adjust with the actual response data model
}