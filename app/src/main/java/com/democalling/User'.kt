package com.democalling

data class User(
    val id: Int,
    val name: String,
    val email: String
)
data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)
