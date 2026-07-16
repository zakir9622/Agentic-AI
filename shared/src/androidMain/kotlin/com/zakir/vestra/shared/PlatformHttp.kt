package com.zakir.vestra.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

/** Default HTTP client for this platform; keeps engine artifacts out of the app module. */
fun platformHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        // Cloud try-on runs take up to ~2 minutes; pack downloads stream for longer.
        requestTimeoutMillis = 180_000
        connectTimeoutMillis = 20_000
        socketTimeoutMillis = 180_000
    }
}
