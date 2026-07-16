package com.zakir.vestra.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/** Default HTTP client for this platform; keeps engine artifacts out of the app module. */
fun platformHttpClient(): HttpClient = HttpClient(OkHttp)
