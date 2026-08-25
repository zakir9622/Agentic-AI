package com.zakir.vestra.shared.content

/** One `## <version>` section of `CHANGELOG.md`. */
data class ChangelogRelease(val version: String, val body: String)

/**
 * Parses the app's real `CHANGELOG.md` into a release list for the in-app Changelog screen
 * (A4.10) — pure text parsing, no Android dependency, so it's directly testable and shared with
 * whatever loads the bundled asset text. Deliberately does not hand-maintain a parallel release
 * list: parsing the real file is the only way the in-app list can't drift out of sync with what
 * actually shipped, matching this project's anti-fabrication discipline.
 */
object ChangelogParser {
    /** A release heading starts with a digit (`3.1.3`, `3.1.0 (stable)`, `2.7.3–2.7.6`, ...). */
    private val VERSION_HEADING = Regex("^## (\\d.*)$")

    /**
     * Splits on top-level `## <version>` headings; everything else is that release's body.
     * Non-version `## ` headings (e.g. this file's own `## CI / releases` note) are not
     * releases and are dropped entirely, rather than being folded into whichever release
     * happens to precede them — this codebase's changelog isn't only version sections.
     */
    fun parse(markdown: String): List<ChangelogRelease> {
        val releases = mutableListOf<ChangelogRelease>()
        var currentVersion: String? = null
        val currentBody = StringBuilder()

        fun flush() {
            val version = currentVersion ?: return
            releases += ChangelogRelease(version, currentBody.toString().trim())
            currentBody.clear()
        }

        for (line in markdown.lines()) {
            val versionMatch = VERSION_HEADING.find(line)
            when {
                versionMatch != null -> {
                    flush()
                    currentVersion = versionMatch.groupValues[1].trim()
                }
                line.startsWith("## ") -> {
                    flush()
                    currentVersion = null
                }
                currentVersion != null -> currentBody.append(line).append('\n')
            }
        }
        flush()
        return releases
    }
}
