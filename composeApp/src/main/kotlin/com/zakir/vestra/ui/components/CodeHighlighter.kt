package com.zakir.vestra.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Colours for one code theme. Kept separate from [VestraCodeTheme] so a caller can tint a block
 * to a surface's accent without rewriting the tokenizer.
 */
data class CodeTheme(
    val plain: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val property: Color,
    val punctuation: Color,
)

/**
 * A deliberately small, regex-free tokenizer for rendering code inside chat bubbles.
 *
 * This is **presentation only**. It exists because a code answer rendered as one flat grey blob
 * is unreadable at chat-bubble size, not because the app needs a parser — it has no language
 * server, and pulling a real highlighting library in for a handful of lines in a message would be
 * a heavy dependency for a cosmetic gain. It handles the constructs that actually show up in
 * generated snippets — line and block comments, quoted strings, numbers with units, CSS-style
 * `property:` keys, and a shared keyword set — and falls back to plain text for anything else.
 *
 * It does not attempt to be correct for every language. Where it guesses wrong the worst outcome
 * is a token in the wrong colour, never mangled or dropped source: every character of the input
 * is emitted exactly once, which [CodeHighlighterTest] pins.
 */
object CodeHighlighter {

    private val keywords = setOf(
        // Shared across the languages this app's code studio actually emits.
        "fun", "val", "var", "class", "object", "interface", "return", "if", "else", "when",
        "for", "while", "do", "try", "catch", "finally", "throw", "import", "package", "private",
        "internal", "public", "override", "suspend", "data", "sealed", "enum", "companion",
        "const", "null", "true", "false", "this", "super", "is", "as", "in", "by", "lateinit",
        "function", "let", "const", "def", "lambda", "None", "True", "False", "elif", "async",
        "await", "export", "default", "new", "typeof", "instanceof", "extends", "implements",
        "public", "static", "void", "int", "float", "double", "boolean", "String",
    )

    fun highlight(code: String, theme: CodeTheme): AnnotatedString = buildAnnotatedString {
        var i = 0
        val n = code.length

        fun span(color: Color, weight: FontWeight? = null, from: Int, to: Int) {
            pushStyle(SpanStyle(color = color, fontWeight = weight))
            append(code.substring(from, to))
            pop()
        }

        while (i < n) {
            val c = code[i]
            when {
                // Line comment — // or # to end of line.
                c == '/' && i + 1 < n && code[i + 1] == '/' -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) n else it }
                    span(theme.comment, from = i, to = end); i = end
                }
                c == '#' -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) n else it }
                    span(theme.comment, from = i, to = end); i = end
                }
                // Block comment.
                c == '/' && i + 1 < n && code[i + 1] == '*' -> {
                    val close = code.indexOf("*/", i + 2)
                    val end = if (close == -1) n else close + 2
                    span(theme.comment, from = i, to = end); i = end
                }
                // Quoted string. An unterminated quote runs to end-of-input rather than throwing.
                c == '"' || c == '\'' || c == '`' -> {
                    var j = i + 1
                    while (j < n && code[j] != c) {
                        if (code[j] == '\\') j++
                        j++
                    }
                    val end = (j + 1).coerceAtMost(n)
                    span(theme.string, from = i, to = end); i = end
                }
                c.isDigit() -> {
                    var j = i
                    // Trailing unit characters (px, rem, f, L) stay part of the number token.
                    while (j < n && (code[j].isDigit() || code[j] == '.' || code[j].isLetter())) j++
                    span(theme.number, from = i, to = j); i = j
                }
                c.isLetter() || c == '_' || c == '-' || c == '@' || c == '$' -> {
                    var j = i
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '-' ||
                            code[j] == '@' || code[j] == '$')
                    ) {
                        j++
                    }
                    val word = code.substring(i, j)
                    // A word immediately followed by ':' is a key — CSS property, JSON field,
                    // named argument. Colouring those separately is most of what makes a snippet
                    // scannable at this size.
                    val isKey = j < n && code[j] == ':'
                    val color = when {
                        word in keywords -> theme.keyword
                        isKey -> theme.property
                        else -> theme.plain
                    }
                    val weight = if (word in keywords) FontWeight.SemiBold else null
                    span(color, weight, i, j); i = j
                }
                c == '{' || c == '}' || c == '(' || c == ')' || c == '[' || c == ']' ||
                    c == ';' || c == ':' || c == ',' || c == '.' || c == '=' -> {
                    span(theme.punctuation, from = i, to = i + 1); i++
                }
                else -> {
                    append(c); i++
                }
            }
        }
    }
}
