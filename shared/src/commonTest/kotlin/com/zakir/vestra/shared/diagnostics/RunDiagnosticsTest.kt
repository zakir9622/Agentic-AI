package com.zakir.vestra.shared.diagnostics

import com.zakir.vestra.shared.testutil.TestMemorySettings
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.usage.UsageSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunDiagnosticsTest {

    @Test
    fun appendAndExportRoundTrip() {
        val diag = RunDiagnostics(TestMemorySettings())
        val builder = diag.startRun(
            capability = RunCapability.IMAGE_GEN,
            tier = EngineTier.CLOUD,
            modelLabel = "FLUX",
            deviceRamMb = 8192,
        )
        builder.stage("connect", 120)
        builder.stage("generate", 3400)
        builder.complete(success = true, note = "flux-schnell-hf")
        val records = diag.records.value
        assertEquals(1, records.size)
        assertTrue(records[0].success)
        assertEquals(2, records[0].stages.size)
        assertTrue(diag.exportJson().contains("IMAGE_GEN"))
    }

    @Test
    fun exportBundleIncludesUsageLedger() {
        val diag = RunDiagnostics(TestMemorySettings())
        diag.startRun(RunCapability.CHAT, modelLabel = "Groq").complete(success = true)
        val usage = UsageSummary(totalRequests = 3, successCount = 2)
        val bundle = diag.exportBundle(usage)
        assertTrue(bundle.contains("usageLedger"))
        assertTrue(bundle.contains("totalRequests"))
    }

    @Test
    fun exportBundleIncludesOptionalLogcatAndVersion() {
        val diag = RunDiagnostics(TestMemorySettings())
        diag.startRun(RunCapability.TRY_ON, tier = EngineTier.LITE).complete(success = true)
        val bundle = diag.exportBundle(
            logcatSnippet = "W/Lookbook: sample warning line",
            appVersion = "3.0.8 (50)",
        )
        assertTrue(bundle.contains("logcatSnippet"))
        assertTrue(bundle.contains("sample warning line"))
        assertTrue(bundle.contains("3.0.8 (50)"))
    }

    @Test
    fun startRunHonorsASuppliedId() {
        val diag = RunDiagnostics(TestMemorySettings())
        val builder = diag.startRun(capability = RunCapability.CODE, id = "external-correlation-id")
        assertEquals("external-correlation-id", builder.id)
        builder.complete(success = true)
        assertEquals("external-correlation-id", diag.records.value.first().id)
    }

    @Test
    fun startRunGeneratesAnIdWhenNoneIsSupplied() {
        val diag = RunDiagnostics(TestMemorySettings())
        val builder = diag.startRun(capability = RunCapability.CODE)
        assertTrue(builder.id.endsWith("-CODE"))
        assertNotEquals("external-correlation-id", builder.id)
    }

    @Test
    fun warningStageSurvivesReloadFromPersistedSettings() {
        val settings = TestMemorySettings()
        val diag = RunDiagnostics(settings)
        val builder = diag.startRun(capability = RunCapability.IMAGE_EDIT)
        builder.stage("Vision assist skipped — pack broken", 12, isWarning = true)
        builder.stage("Encoding prompt", 40)
        builder.complete(success = true)

        val reloaded = RunDiagnostics(settings)
        val stages = reloaded.records.value.first().stages
        assertTrue(stages[0].isWarning)
        assertTrue(!stages[1].isWarning)
    }
}
