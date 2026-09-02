package com.zakir.vestra.ui.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.zakir.vestra.ui.components.GlassSnackbar
import com.zakir.vestra.ui.components.SnackbarLevel
import java.util.Locale

/**
 * Read-aloud for an assistant reply, backed by the platform [TextToSpeech] engine.
 *
 * The reference app puts a speaker on every reply and this is the honest way to provide it: the
 * device's own TTS, initialised once per screen and shut down with it. It is deliberately *not*
 * routed through the app's Audio studio — that generates new speech from a cloud or on-device
 * model and would bill a request to re-read text the user already has.
 *
 * Every failure path is visible rather than silent. An engine that never initialises, a missing
 * voice for the current locale, or a device with no TTS installed all say so, because a speaker
 * button that does nothing when tapped is worse than one that isn't there.
 */
class Speaker internal constructor(
    private val engine: TextToSpeech,
    private val ready: () -> Boolean,
) {
    fun toggle(text: String) {
        if (!ready()) {
            GlassSnackbar.show("No text-to-speech engine is available on this device", SnackbarLevel.WARNING)
            return
        }
        if (engine.isSpeaking) {
            engine.stop()
            return
        }
        val body = text.trim()
        if (body.isEmpty()) return
        // QUEUE_FLUSH, not QUEUE_ADD: tapping speak on a second reply should replace the first,
        // not queue behind several minutes of it.
        engine.speak(body, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }
}

/** Creates a [Speaker] bound to the composition's lifetime. */
@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val engine = remember {
        var created: TextToSpeech? = null
        created = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            val result = created?.setLanguage(Locale.getDefault())
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
        created
    }
    DisposableEffect(engine) {
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }
    return remember(engine) { Speaker(engine) { ready } }
}

/** Shares a reply as plain text through the system chooser. */
fun shareText(context: Context, text: String, subject: String = "From The Lookbook") {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(send, "Share reply").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        GlassSnackbar.show("Nothing on this device can share text", SnackbarLevel.WARNING)
    }
}

/**
 * The intent that opens the system's dictation UI, or null when no recogniser is installed.
 *
 * Returning null is what lets the composer *hide* its mic rather than show one that opens
 * nothing. Using [RecognizerIntent] rather than `SpeechRecognizer` is also what keeps dictation
 * free of a `RECORD_AUDIO` grant: the recording happens in the system's own activity.
 */
fun dictationIntent(context: Context, promptLabel: String): Intent? {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PROMPT, promptLabel)
    }
    @Suppress("DEPRECATION")
    val resolvable = context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    return if (resolvable) intent else null
}
