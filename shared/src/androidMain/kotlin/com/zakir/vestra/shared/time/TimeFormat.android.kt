package com.zakir.vestra.shared.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val hmsFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss", Locale.US) }

actual fun formatHms(epochMs: Long): String = hmsFormat.get()!!.format(Date(epochMs))
