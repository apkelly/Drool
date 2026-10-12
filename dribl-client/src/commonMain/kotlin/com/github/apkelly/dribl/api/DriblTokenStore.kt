package com.github.apkelly.dribl.api

interface DriblTokenStore {
    suspend fun getBearerToken(): String?

    suspend fun saveBearerToken(
        token: String,
    )
}
