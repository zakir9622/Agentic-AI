package com.zakir.vestra.ui.screens.news

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Splitting a reply into prose and code runs.
 *
 * The invariant worth pinning is that **no content is lost**. A message is the model's answer;
 * dropping a paragraph or half a snippet because a fence was malformed would be silent data loss
 * in the one place the user cannot recover it from.
 */
class MessageSegmentTest {

    @Test
    fun `a message with no fence is a single prose segment`() {
        val out = MessageSegment.split("just some words")
        assertEquals(1, out.size)
        assertEquals("just some words", (out[0] as MessageSegment.Prose).text)
    }

    @Test
    fun `prose before and after a fence is preserved`() {
        val out = MessageSegment.split("Here you go:\n```css\n.a { color: red; }\n```\nHope that helps.")
        assertEquals(3, out.size)
        assertEquals("Here you go:", (out[0] as MessageSegment.Prose).text)
        val code = (out[1] as MessageSegment.Code)
        assertEquals("css", code.language)
        assertEquals(".a { color: red; }", code.code)
        assertEquals("Hope that helps.", (out[2] as MessageSegment.Prose).text)
    }

    @Test
    fun `a fence with no language tag still yields a code segment`() {
        val out = MessageSegment.split("```\nplain\n```")
        val code = out.single() as MessageSegment.Code
        assertEquals(null, code.language)
        assertEquals("plain", code.code)
    }

    @Test
    fun `an unterminated fence keeps the remaining text as code rather than dropping it`() {
        val out = MessageSegment.split("Try:\n```kotlin\nval a = 1\nval b = 2")
        assertEquals(2, out.size)
        val code = (out[1] as MessageSegment.Code)
        assertTrue("first line kept", code.code.contains("val a = 1"))
        assertTrue("last line kept — an open fence must not truncate", code.code.contains("val b = 2"))
    }

    @Test
    fun `two fenced blocks in one message both survive`() {
        val out = MessageSegment.split("one\n```\nA\n```\ntwo\n```\nB\n```\nthree")
        val codes = out.filterIsInstance<MessageSegment.Code>().map { it.code }
        assertEquals(listOf("A", "B"), codes)
        val prose = out.filterIsInstance<MessageSegment.Prose>().map { it.text }
        assertEquals(listOf("one", "two", "three"), prose)
    }

    @Test
    fun `an empty fence contributes no code segment`() {
        val out = MessageSegment.split("before\n```\n\n```\nafter")
        assertTrue("an empty block is not worth a code surface", out.none { it is MessageSegment.Code })
        assertEquals(listOf("before", "after"), out.filterIsInstance<MessageSegment.Prose>().map { it.text })
    }

    @Test
    fun `a blank message never yields an empty list`() {
        assertEquals("callers iterate the result; it must never be empty", 1, MessageSegment.split("").size)
    }
}
