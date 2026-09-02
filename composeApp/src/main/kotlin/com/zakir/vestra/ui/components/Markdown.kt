package com.zakir.vestra.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The small slice of Markdown that language models actually emit in chat replies.
 *
 * Every cloud chat model this app talks to answers in Markdown whether or not it was asked to.
 * The thread used to render that reply with a plain [Text], so a perfectly ordinary answer
 * arrived on screen as:
 *
 * ```
 * - **Fashion try-on** features and tips
 * - **On-device AI** (Lite/Pro packs) vs cloud models
 * ```
 *
 * — asterisks and hyphens intact, no bullets, no bold. That is the single most visible quality
 * defect in the app, because it is on the first reply of every conversation.
 *
 * Scope is deliberately narrow: headings, bullets, ordered items, blockquotes, horizontal rules,
 * and the inline runs (`**bold**`, `*italic*`, `` `code` ``, `~~strike~~`, `[text](url)`). Fenced
 * code is *not* handled here — [com.zakir.vestra.ui.screens.news.MessageSegment] splits those out
 * first and routes them to [CodeBlock], which can scroll and syntax-colour them.
 *
 * The invariant the tests pin is that **no visible text is lost**. A parser that silently drops a
 * sentence because a marker was unbalanced would be worse than showing the raw asterisks: the
 * user can read around stray punctuation, but cannot recover a paragraph that never rendered. So
 * every unmatched marker falls back to being literal text.
 */
sealed interface MarkdownBlock {
    val spans: AnnotatedString

    /** Ordinary paragraph. */
    data class Paragraph(override val spans: AnnotatedString) : MarkdownBlock

    /** `# ` through `###### `. [level] is 1-6. */
    data class Heading(val level: Int, override val spans: AnnotatedString) : MarkdownBlock

    /** `- `, `* ` or `+ `. [indent] counts nesting steps, 0 for a top-level item. */
    data class Bullet(val indent: Int, override val spans: AnnotatedString) : MarkdownBlock

    /** `1. `, `2)` … [number] is the literal marker the model wrote, not a re-count. */
    data class Ordered(val indent: Int, val number: String, override val spans: AnnotatedString) : MarkdownBlock

    /** `> `. */
    data class Quote(override val spans: AnnotatedString) : MarkdownBlock

    /** `---`, `***` or `___` on its own line. */
    data object Rule : MarkdownBlock {
        override val spans: AnnotatedString get() = AnnotatedString("")
    }
}

object Markdown {

    private val HEADING = Regex("""^(#{1,6})\s+(.*)$""")
    private val BULLET = Regex("""^(\s*)[-*+]\s+(.*)$""")
    private val ORDERED = Regex("""^(\s*)(\d{1,3})[.)]\s+(.*)$""")
    private val QUOTE = Regex("""^>\s?(.*)$""")
    private val RULE = Regex("""^\s*([-*_])\s*\1\s*\1[\s\-*_]*$""")

    /**
     * Split [text] into renderable blocks.
     *
     * Consecutive non-empty lines that are not themselves markers are joined into one paragraph,
     * matching how Markdown treats a soft wrap — models hard-wrap prose constantly, and rendering
     * each wrapped line as its own paragraph produced ragged double-spacing in the bubble.
     */
    fun parse(text: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = StringBuilder()

        fun flush() {
            val body = paragraph.toString().trim()
            paragraph.setLength(0)
            if (body.isNotEmpty()) blocks += MarkdownBlock.Paragraph(inline(body))
        }

        text.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> flush()

                RULE.matches(line) -> {
                    flush()
                    blocks += MarkdownBlock.Rule
                }

                HEADING.matches(line) -> {
                    flush()
                    val m = HEADING.find(line)!!
                    blocks += MarkdownBlock.Heading(m.groupValues[1].length, inline(m.groupValues[2]))
                }

                ORDERED.matches(line) -> {
                    flush()
                    val m = ORDERED.find(line)!!
                    blocks += MarkdownBlock.Ordered(
                        indent = m.groupValues[1].length / 2,
                        number = m.groupValues[2],
                        spans = inline(m.groupValues[3]),
                    )
                }

                BULLET.matches(line) -> {
                    flush()
                    val m = BULLET.find(line)!!
                    blocks += MarkdownBlock.Bullet(m.groupValues[1].length / 2, inline(m.groupValues[2]))
                }

                QUOTE.matches(line) -> {
                    flush()
                    blocks += MarkdownBlock.Quote(inline(QUOTE.find(line)!!.groupValues[1]))
                }

                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line.trim())
                }
            }
        }
        flush()
        return blocks
    }

    /**
     * Inline emphasis within one line.
     *
     * Hand-scanned rather than regex-driven so an unmatched opener can degrade to literal text
     * instead of eating the rest of the line — `2 * 3 * 4` must not become italic, and a lone
     * `**` at the end of a streamed token must not swallow what follows.
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LongMethod")
    fun inline(source: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                // `code`
                c == '`' -> {
                    val end = source.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withSpan(CODE_SPAN) { append(source.substring(i + 1, end)) }
                        i = end + 1
                    } else {
                        append(c); i++
                    }
                }

                // **bold** / __bold__
                (c == '*' || c == '_') && i + 1 < source.length && source[i + 1] == c -> {
                    val marker = "$c$c"
                    val end = source.indexOf(marker, i + 2)
                    if (end > i + 2) {
                        withSpan(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(inline(source.substring(i + 2, end)))
                        }
                        i = end + 2
                    } else {
                        append(marker); i += 2
                    }
                }

                // *italic* / _italic_ — a marker with whitespace on the inner edge is arithmetic
                // or a stray underscore in an identifier, not emphasis.
                (c == '*' || c == '_') -> {
                    val end = source.indexOf(c, i + 1)
                    val inner = if (end > i + 1) source.substring(i + 1, end) else null
                    if (inner != null && inner.isNotBlank() && !inner.first().isWhitespace() && !inner.last().isWhitespace()) {
                        withSpan(SpanStyle(fontStyle = FontStyle.Italic)) { append(inline(inner)) }
                        i = end + 1
                    } else {
                        append(c); i++
                    }
                }

                // ~~strike~~
                c == '~' && i + 1 < source.length && source[i + 1] == '~' -> {
                    val end = source.indexOf("~~", i + 2)
                    if (end > i + 2) {
                        withSpan(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(inline(source.substring(i + 2, end)))
                        }
                        i = end + 2
                    } else {
                        append("~~"); i += 2
                    }
                }

                // [label](target) — the label renders underlined; the target is dropped rather
                // than made tappable, because a model-authored URL is untrusted and this bubble
                // is not the place to hand the user a one-tap navigation to it.
                c == '[' -> {
                    val close = source.indexOf(']', i + 1)
                    val open = if (close > 0) close + 1 else -1
                    if (close > i && open < source.length && open > 0 && source[open] == '(') {
                        val end = source.indexOf(')', open + 1)
                        if (end > open) {
                            withSpan(LINK_SPAN) { append(inline(source.substring(i + 1, close))) }
                            i = end + 1
                        } else {
                            append(c); i++
                        }
                    } else {
                        append(c); i++
                    }
                }

                else -> {
                    append(c); i++
                }
            }
        }
    }

    private val CODE_SPAN = SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    private val LINK_SPAN = SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)

    private inline fun androidx.compose.ui.text.AnnotatedString.Builder.withSpan(
        style: SpanStyle,
        block: () -> Unit,
    ) {
        val start = length
        block()
        addStyle(style, start, length)
    }
}

/**
 * Renders a Markdown reply. Prose only — pass fenced blocks to [CodeBlock] instead.
 *
 * [color] applies to every block; emphasis is carried by weight, slant and decoration rather
 * than by hue, so a reply stays legible against both the user bubble's accent fill and the
 * assistant side's canvas.
 */
@Composable
fun MarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium.copy(
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp,
    ),
) {
    val blocks = remember(text) { Markdown.parse(text) }
    Column(modifier) {
        blocks.forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(if (block is MarkdownBlock.Heading) 10.dp else 6.dp))
            when (block) {
                is MarkdownBlock.Paragraph -> Text(block.spans, style = style, color = color)

                is MarkdownBlock.Heading -> Text(
                    block.spans,
                    style = style.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = when (block.level) {
                            1 -> 19.sp
                            2 -> 17.sp
                            else -> 15.sp
                        },
                    ),
                    color = color,
                )

                is MarkdownBlock.Bullet -> MarkerRow(
                    marker = "•",
                    indent = block.indent,
                    spans = block.spans,
                    style = style,
                    color = color,
                )

                is MarkdownBlock.Ordered -> MarkerRow(
                    marker = "${block.number}.",
                    indent = block.indent,
                    spans = block.spans,
                    style = style,
                    color = color,
                )

                is MarkdownBlock.Quote -> Row(Modifier.fillMaxWidth()) {
                    Spacer(
                        Modifier
                            .width(3.dp)
                            .height(0.dp)
                            .padding(end = 8.dp),
                    )
                    Text(
                        block.spans,
                        style = style.copy(fontStyle = FontStyle.Italic),
                        color = color.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }

                MarkdownBlock.Rule -> Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

/** One list item: a fixed-width marker gutter so wrapped lines align under the text, not the dot. */
@Composable
private fun MarkerRow(
    marker: String,
    indent: Int,
    spans: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = (indent.coerceIn(0, 3) * 14).dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            marker,
            style = style,
            color = color.copy(alpha = 0.75f),
            modifier = Modifier.width(if (marker.length > 2) 24.dp else 16.dp),
        )
        Text(spans, style = style, color = color, modifier = Modifier.weight(1f))
    }
}
