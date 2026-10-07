package com.dcsxgdg.soundwave.data

import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ITunesRepository {
    suspend fun searchSongs(query: String = "instrumental music"): List<Song> =
        withContext(Dispatchers.IO) {
            val requestUrl = "https://itunes.apple.com/search".toUri()
                .buildUpon()
                .appendQueryParameter("term", query)
                .appendQueryParameter("entity", "song")
                .appendQueryParameter("limit", "25")
                .build()

            val connection = (URL(requestUrl.toString()).openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                }

            try {
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    throw IOException("Music catalog returned HTTP $status")
                }

                try {
                    val results = JSONObject(body).optJSONArray("results")
                        ?: throw IOException("Music catalog response did not contain a results list")

                    buildList {
                        for (index in 0 until results.length()) {
                            val item = results.optJSONObject(index) ?: continue
                            val id = item.optLong("trackId").takeIf { it > 0 } ?: continue
                            val title = item.optString("trackName").takeIf(String::isNotBlank) ?: continue
                            val artist = item.optString("artistName").takeIf(String::isNotBlank)
                                ?: "Unknown artist"
                            add(
                                Song(
                                    id = "itunes-$id",
                                    title = title,
                                    artist = artist,
                                    artworkUrl = item.optString("artworkUrl100").takeIf(String::isNotBlank),
                                    storeUrl = item.optString("trackViewUrl").takeIf(String::isNotBlank),
                                ),
                            )
                        }
                    }
                } catch (error: JSONException) {
                    throw IOException("Music catalog returned invalid JSON.", error)
                }
            } finally {
                connection.disconnect()
            }
        }
}
