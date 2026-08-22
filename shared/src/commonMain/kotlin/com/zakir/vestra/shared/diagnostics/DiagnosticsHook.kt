package com.zakir.vestra.shared.diagnostics

import com.zakir.vestra.shared.domain.EngineTier

/**
 * Optional bridge so engines can emit structured stage timings without constructor churn.
 * Set once from [com.zakir.vestra.VestraApp] on startup.
 */
object DiagnosticsHook {
    var store: RunDiagnostics? = null
    var deviceRamMb: Long? = null

    private var activeTryOn: RunDiagnostics.RunBuilder? = null

    fun startTryOn(
        tier: EngineTier,
        modelLabel: String? = null,
        deviceRamMb: Long? = null,
    ): RunDiagnostics.RunBuilder? {
        val builder = store?.startRun(
            capability = RunCapability.TRY_ON,
            tier = tier,
            modelLabel = modelLabel ?: tier.name,
            deviceRamMb = deviceRamMb ?: this.deviceRamMb,
        )
        activeTryOn = builder
        return builder
    }

    fun stage(builder: RunDiagnostics.RunBuilder?, name: String, sinceMs: Long, detail: String = "") {
        builder?.stage(name, System.currentTimeMillis() - sinceMs, detail)
    }

    fun stage(name: String, sinceMs: Long, detail: String = "") {
        stage(activeTryOn, name, sinceMs, detail)
    }

    fun completeTryOn(success: Boolean, error: String? = null, note: String = "") {
        activeTryOn?.complete(success, error, note)
        activeTryOn = null
    }
}
