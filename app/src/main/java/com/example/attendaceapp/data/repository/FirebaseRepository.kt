package com.example.attendaceapp.data.repository

@Deprecated(
    message = "FirebaseRepository sudah dimigrasikan ke API. Gunakan ApiRepository.",
    replaceWith = ReplaceWith("ApiRepository")
)
typealias FirebaseRepository = ApiRepository
