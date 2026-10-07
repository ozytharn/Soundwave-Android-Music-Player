package com.dcsxgdg.soundwave.data

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val storeUrl: String?,
    val localAudioResource: Int? = null,
    val isLocal: Boolean = false,
)

object DemoSongs {
    val songs = listOf(
        Song(
            id = "merfolk",
            title = "The Merfolk I Should Turn To Be",
            artist = "Soft and Furious",
            artworkUrl = null,
            storeUrl = null,
            localAudioResource = com.dcsxgdg.soundwave.R.raw.merfolk,
            isLocal = true,
        ),
        Song("paper-lanterns", "Paper Lanterns", "DCS x GDG Sessions", null, null),
        Song("blue-hour", "Blue Hour", "DCS x GDG Sessions", null, null),
        Song("quiet-tides", "Quiet Tides", "DCS x GDG Sessions", null, null),
        Song("first-light", "First Light", "DCS x GDG Sessions", null, null),
    )
}
