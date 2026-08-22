package com.zakir.vestra.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class NewsFeedEntry(
    val source: String,
    val url: String,
)

object NewsFeedConfig {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<Pair<String, String>> =
        runCatching {
            context.assets.open("news_feeds.json").bufferedReader().use { reader ->
                json.decodeFromString<List<NewsFeedEntry>>(reader.readText())
                    .map { it.source to it.url }
            }
        }.getOrElse { NewsRepositoryDefaults.FEEDS }

    /** Fallback when assets are missing (tests / previews). */
    object NewsRepositoryDefaults {
        val FEEDS = listOf(
            "BBC Tech" to "https://feeds.bbci.co.uk/news/technology/rss.xml",
            "Hugging Face" to "https://huggingface.co/blog/feed.xml",
        )
    }
}
