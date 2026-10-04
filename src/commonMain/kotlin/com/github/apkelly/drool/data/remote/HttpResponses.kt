package com.github.apkelly.drool.data.remote

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.SerializationException

internal suspend inline fun <reified T> HttpResponse.requireBody(url: String): T {
    if (!status.isSuccess()) {
        throw DriblHttpException(status.value, url, bodyAsText())
    }
    return try {
        body()
    } catch (error: JsonConvertException) {
        throw DriblResponseException(
            "Response from $url did not match ${T::class.simpleName}: ${error.message}"
        )
    } catch (error: SerializationException) {
        throw DriblResponseException(
            "Response from $url did not match ${T::class.simpleName}: ${error.message}"
        )
    }
}

internal suspend fun HttpResponse.requireSuccess(url: String) {
    if (!status.isSuccess()) {
        throw DriblHttpException(status.value, url, bodyAsText())
    }
}
