package com.zakir.vestra.ui.screens.wardrobe

import com.zakir.vestra.shared.content.LookbookCopy
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.wardrobe.WardrobeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure filter-logic tests for A4.4's media-type filter — no `WardrobeRepository`/Compose
 * harness needed since [filterWardrobeEntries] is a plain function over a list.
 */
class WardrobeFilterTest {

    private fun entry(id: String, path: String, favorited: Boolean = false) = WardrobeEntry(
        id = id,
        createdAtEpochMillis = 0L,
        imagePath = path,
        garmentUri = "",
        personLabel = "",
        tier = EngineTier.LITE,
        favorited = favorited,
    )

    private val image1 = entry("img1", "/looks/a.png")
    private val image2 = entry("img2", "/looks/b.jpg", favorited = true)
    private val video1 = entry("vid1", "/looks/c.mp4")
    private val video2 = entry("vid2", "/looks/d.webm", favorited = true)
    private val all = listOf(image1, image2, video1, video2)

    @Test
    fun isVideoEntry_detectsMp4AndWebm() {
        assertEquals(true, video1.isVideoEntry())
        assertEquals(true, video2.isVideoEntry())
        assertEquals(false, image1.isVideoEntry())
    }

    @Test
    fun isVideoEntry_caseInsensitiveExtension() {
        val upper = entry("v", "/looks/e.MP4")
        assertEquals(true, upper.isVideoEntry())
    }

    @Test
    fun filter_allTypesAllFavorites_returnsEverything() {
        val result = filterWardrobeEntries(all, favoritesOnly = false, mediaFilter = WardrobeMediaFilter.ALL)
        assertEquals(all, result)
    }

    @Test
    fun filter_imagesOnly_excludesVideos() {
        val result = filterWardrobeEntries(all, favoritesOnly = false, mediaFilter = WardrobeMediaFilter.IMAGES)
        assertEquals(listOf(image1, image2), result)
    }

    @Test
    fun filter_videosOnly_excludesImages() {
        val result = filterWardrobeEntries(all, favoritesOnly = false, mediaFilter = WardrobeMediaFilter.VIDEOS)
        assertEquals(listOf(video1, video2), result)
    }

    @Test
    fun filter_favoritesOnly_excludesUnfavorited() {
        val result = filterWardrobeEntries(all, favoritesOnly = true, mediaFilter = WardrobeMediaFilter.ALL)
        assertEquals(listOf(image2, video2), result)
    }

    @Test
    fun filter_favoritesAndVideos_composesBothFilters() {
        val result = filterWardrobeEntries(all, favoritesOnly = true, mediaFilter = WardrobeMediaFilter.VIDEOS)
        assertEquals(listOf(video2), result)
    }

    @Test
    fun filter_favoritesAndImages_composesBothFilters() {
        val result = filterWardrobeEntries(all, favoritesOnly = true, mediaFilter = WardrobeMediaFilter.IMAGES)
        assertEquals(listOf(image2), result)
    }

    @Test
    fun filter_noMatches_returnsEmptyList() {
        val onlyImages = listOf(image1)
        val result = filterWardrobeEntries(onlyImages, favoritesOnly = false, mediaFilter = WardrobeMediaFilter.VIDEOS)
        assertEquals(emptyList<WardrobeEntry>(), result)
    }

    @Test
    fun filter_emptyInput_returnsEmptyList() {
        val result = filterWardrobeEntries(emptyList(), favoritesOnly = false, mediaFilter = WardrobeMediaFilter.ALL)
        assertEquals(emptyList<WardrobeEntry>(), result)
    }

    @Test
    fun emptyMessage_favoritesOnly_mentionsFavorites() {
        assertEquals(
            LookbookCopy.EMPTY_FAVORITES,
            wardrobeEmptyMessage(favoritesOnly = true, mediaFilter = WardrobeMediaFilter.ALL),
        )
    }

    @Test
    fun emptyMessage_imagesOnly_mentionsImages() {
        assertEquals(
            LookbookCopy.EMPTY_IMAGES_FILTER,
            wardrobeEmptyMessage(favoritesOnly = false, mediaFilter = WardrobeMediaFilter.IMAGES),
        )
    }

    @Test
    fun emptyMessage_videosOnly_mentionsVideos() {
        assertEquals(
            LookbookCopy.EMPTY_VIDEOS_FILTER,
            wardrobeEmptyMessage(favoritesOnly = false, mediaFilter = WardrobeMediaFilter.VIDEOS),
        )
    }

    @Test
    fun emptyMessage_favoritesAndImages_mentionsBothNotJustFavorites() {
        val message = wardrobeEmptyMessage(favoritesOnly = true, mediaFilter = WardrobeMediaFilter.IMAGES)
        assertEquals(LookbookCopy.EMPTY_FAVORITE_IMAGES, message)
        // Regression guard for the exact bug found in review: combining both filters must not
        // silently fall back to the plain "no favorites" message, which would misreport why
        // the list is actually empty (favorites exist, just none match the type filter).
        assertNotEquals(LookbookCopy.EMPTY_FAVORITES, message)
    }

    @Test
    fun emptyMessage_favoritesAndVideos_mentionsBothNotJustFavorites() {
        val message = wardrobeEmptyMessage(favoritesOnly = true, mediaFilter = WardrobeMediaFilter.VIDEOS)
        assertEquals(LookbookCopy.EMPTY_FAVORITE_VIDEOS, message)
        assertNotEquals(LookbookCopy.EMPTY_FAVORITES, message)
    }
}
