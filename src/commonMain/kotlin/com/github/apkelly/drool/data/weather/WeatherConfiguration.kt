package com.github.apkelly.drool.data.weather

expect fun platformWeatherApiKey(): String?

expect fun platformWeatherRequestHeaders(): Map<String, String>
