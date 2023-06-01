package com.spectre7.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.DEFAULT_CONNECT_TIMEOUT
import com.spectre7.spmp.api.YoutubeVideoFormat
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.api.getVideoFormats
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.platform.crop
import com.spectre7.spmp.platform.toImageBitmap
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.SongPreviewLong
import com.spectre7.spmp.ui.component.SongPreviewSquare
import com.spectre7.utils.ValueListeners
import com.spectre7.utils.lazyAssert
import com.spectre7.utils.toHiragana
import okhttp3.internal.filterList
import java.io.FileNotFoundException
import java.net.URL

class SongItemData(override val data_item: Song): MediaItemData(data_item) {

    var song_type: Song.SongType? by mutableStateOf(null)
        private set

    fun supplySongType(value: Song.SongType?, certain: Boolean = false, cached: Boolean = false): Song {
        if (value != song_type && (song_type == null || certain)) {
            song_type = value
            onChanged(cached)
        }
        return data_item
    }

    var duration: Long? by mutableStateOf(null)
        private set

    fun supplyDuration(value: Long?, certain: Boolean = false, cached: Boolean = false): Song {
        if (value != duration && (duration == null || certain)) {
            duration = value
            onChanged(cached)
        }
        return data_item
    }

    var album: Playlist? by mutableStateOf(null)
        private set

    fun supplyAlbum(value: Playlist?, certain: Boolean = false, cached: Boolean = false): Song {
        if (value != album && (album == null || certain)) {
            album = value
            onChanged(cached)
        }
        return data_item
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(song_type?.ordinal), klaxon.toJsonString(duration), klaxon.toJsonString(album?.id))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 3)
        data.removeLast()?.also { supplyAlbum(AccountPlaylist.fromId(it as String), cached = true) }
        data.removeLast()?.also { supplyDuration((it as Int).toLong(), cached = true) }
        data.removeLast()?.also { supplySongType(Song.SongType.values()[it as Int], cached = true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}

class Song protected constructor (
    id: String
): MediaItem(id) {

    enum class AudioQuality {
        LOW, MEDIUM, HIGH
    }
    enum class SongType { SONG, VIDEO }
    enum class LikeStatus {
        UNKNOWN, UNAVAILABLE, NEUTRAL, LIKED, DISLIKED;

        val is_available: Boolean get() = when(this) {
            LIKED, DISLIKED, NEUTRAL -> true
            else -> false
        }
    }

    class SongDataRegistryEntry: MediaItemDataRegistry.Entry() {
        var theme_colour: Int? by mutableStateOf(null)
        var thumbnail_rounding: Int? by mutableStateOf(null)

        var lyrics_id: Int? by mutableStateOf(null)
        var lyrics_source: Lyrics.Source? by mutableStateOf(null)

        @Json(ignored = true)
        val lyrics_listeners = ValueListeners<Pair<Int, Lyrics.Source>?>()
        fun updateLyrics(id: Int?, source: Lyrics.Source?) {
            if (id == lyrics_id && source == lyrics_source) {
                return
            }

            lyrics_id = id
            lyrics_source = source

            lyrics_listeners.call(getLyricsData())
        }
        fun getLyricsData(): Pair<Int, Lyrics.Source>? =
            if (lyrics_id != null) Pair(lyrics_id!!, lyrics_source!!)
            else null

        override fun clear() {
            super.clear()
            theme_colour = null
            thumbnail_rounding = null
            lyrics_id = null
            lyrics_source = null
        }
    }

    private var audio_formats: List<YoutubeVideoFormat>? = null
    private var stream_format: YoutubeVideoFormat? = null
    private var download_format: YoutubeVideoFormat? = null

    override val data = SongItemData(this)
    val song_reg_entry: SongDataRegistryEntry = registry_entry as SongDataRegistryEntry
    override fun getDefaultRegistryEntry(): MediaItemDataRegistry.Entry = SongDataRegistryEntry()

    val like_status = SongLikeStatus(id)
    val lyrics = SongLyricsHolder(this)

    val song_type: SongType? get() = data.song_type
    val duration: Long? get() = data.duration
    val album: Playlist? get() = data.album

    fun <T> editSongData(action: SongItemData.() -> T): T {
        val ret = editData {
            action(this as SongItemData)
        }
        return ret
    }

    suspend fun <T> editSongDataSuspend(action: suspend SongItemData.() -> T): T {
        val ret = editDataSuspend {
            action(this as SongItemData)
        }
        return ret
    }

    fun editSongDataManual(action: SongItemData.() -> Unit): SongItemData {
        action(data)
        return data
    }

    data class Lyrics(
        val id: Int,
        val source: Source,
        val sync_type: SyncType,
        val lines: List<List<Term>>
    ) {

        enum class Source {
            PETITLYRICS;

            val readable: String
                get() = when (this) {
                    PETITLYRICS -> getString("lyrics_source_petitlyrics")
                }

            val colour: Color
                get() = when (this) {
                    PETITLYRICS -> Color(0xFFBD0A0F)
                }
        }

        enum class SyncType {
            NONE,
            LINE_SYNC,
            WORD_SYNC;

            val readable: String
                get() = when (this) {
                    NONE -> getString("lyrics_sync_none")
                    LINE_SYNC -> getString("lyrics_sync_line")
                    WORD_SYNC -> getString("lyrics_sync_word")
                }

            companion object {
                fun fromKey(key: String): SyncType {
                    return when (key) {
                        "text" -> NONE
                        "line_sync" -> LINE_SYNC
                        "text_sync" -> WORD_SYNC
                        else -> throw NotImplementedError(key)
                    }
                }

                fun byPriority(): List<SyncType> {
                    return values().toList().reversed()
                }
            }
        }

        data class Term(val subterms: List<Text>, val start: Long? = null, val end: Long? = null) {
            var line_range: LongRange? = null
            var data: Any? = null

            data class Text(val text: String, var furi: String? = null) {
                init {
                    require(text.isNotBlank())

                    if (furi != null) {
                        if (furi == "*") {
                            this.furi = null
                        }
                        else {
                            furi = furi!!.toHiragana()
                            if (furi == text.toHiragana()) {
                                furi = null
                            }
                        }
                    }
                }
            }

            val range: LongRange
                get() = start!! .. end!!

        }

        init {
            lazyAssert {
                for (line in lines) {
                    for (term in line) {
                        if (sync_type != SyncType.NONE && (term.start == null || term.end == null)) {
                            return@lazyAssert false
                        }
                    }
                }
                return@lazyAssert true
            }
        }
    }

    companion object {
        private val songs: MutableMap<String, Song> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Song {
            return songs.getOrPut(id) {
                val song = Song(id)
                song.loadFromCache()
                return@getOrPut song
            }.getOrReplacedWith() as Song
        }

        fun clearStoredItems(): Int {
            val amount = songs.size
            songs.clear()
            return amount
        }

        fun getTargetStreamQuality(): AudioQuality {
            return Settings.getEnum(Settings.KEY_STREAM_AUDIO_QUALITY)
        }

        fun getTargetDownloadQuality(): AudioQuality {
            return Settings.getEnum(Settings.KEY_DOWNLOAD_AUDIO_QUALITY)
        }
    }

    var theme_colour: Color?
        get() = song_reg_entry.theme_colour?.let { Color(it) }
        set(value) {
            editRegistry {
                (it as SongDataRegistryEntry).theme_colour = value?.toArgb()
            }
        }

    override fun canGetThemeColour(): Boolean = theme_colour != null || super.canGetThemeColour()

    override fun getThemeColour(): Color? = theme_colour ?: super.getThemeColour()

    // Expects formats to be sorted by bitrate (descending)
    private fun List<YoutubeVideoFormat>.getByQuality(quality: AudioQuality): YoutubeVideoFormat {
        check(isNotEmpty())
        return when (quality) {
            AudioQuality.HIGH -> firstOrNull { it.audio_only } ?: first()
            AudioQuality.MEDIUM -> {
                val audio_formats = filterList { audio_only }
                if (audio_formats.isNotEmpty()) {
                    audio_formats[audio_formats.size / 2]
                }
                else {
                    get(size / 2)
                }
            }
            AudioQuality.LOW -> lastOrNull { it.audio_only } ?: last()
        }.also { it.matched_quality = quality }
    }

    @Synchronized
    private fun getAudioFormats(): Result<List<YoutubeVideoFormat>> {
        if (audio_formats == null) {
            val result = getVideoFormats(id) { it.audio_only }
            if (result.isFailure) {
                return result.cast()
            }

            if (result.getOrThrow().isEmpty()) {
                return Result.failure(Exception("No formats returned by getVideoFormats($id)"))
            }

            audio_formats = result.getOrThrow().sortedByDescending { it.bitrate }
        }
        return Result.success(audio_formats!!)
    }

    fun getFormatByQuality(quality: AudioQuality): Result<YoutubeVideoFormat> {
        val formats = getAudioFormats()
        if (formats.isFailure) {
            return formats.cast()
        }

        return Result.success(formats.getOrThrow().getByQuality(quality))
    }

    fun getStreamFormat(): Result<YoutubeVideoFormat> {
        val quality: AudioQuality = getTargetStreamQuality()
        if (stream_format?.matched_quality != quality) {
            val formats = getAudioFormats()
            if (formats.isFailure) {
                return formats.cast()
            }

            stream_format = formats.getOrThrow().getByQuality(quality)
        }

        return Result.success(stream_format!!)
    }

    fun getDownloadFormat(): Result<YoutubeVideoFormat> {
        val quality: AudioQuality = getTargetDownloadQuality()
        if (download_format?.matched_quality != quality) {
            val formats = getAudioFormats()
            if (formats.isFailure) {
                return formats.cast()
            }

            download_format = formats.getOrThrow().getByQuality(quality)
        }

        return Result.success(download_format!!)
    }

    override fun canLoadThumbnail(): Boolean = true

    override fun downloadThumbnail(quality: MediaItemThumbnailProvider.Quality): Result<ImageBitmap> {
        // Iterate through getThumbUrl URL and ThumbnailQuality URLs for passed quality and each lower quality
        for (i in 0 .. quality.ordinal + 1) {

            // Some static thumbnails are cropped for some reason
            if (i == 0 && thumbnail_provider !is MediaItemThumbnailProvider.DynamicProvider) {
                continue
            }

            val url = if (i == 0) getThumbUrl(quality) ?: continue else {
                when (MediaItemThumbnailProvider.Quality.values()[quality.ordinal - i + 1]) {
                    MediaItemThumbnailProvider.Quality.LOW -> "https://img.youtube.com/vi/$id/0.jpg"
                    MediaItemThumbnailProvider.Quality.HIGH -> "https://img.youtube.com/vi/$id/maxresdefault.jpg"
                }
            }

            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT

                val stream = connection.getInputStream()
                val bytes = stream.readBytes()
                stream.close()

                val image = bytes.toImageBitmap()
                if (image.width == image.height) {
                    return Result.success(image)
                }

                // Crop image to 1:1
                val size = (image.width * (9f/16f)).toInt()
                return Result.success(image.crop((image.width - size) / 2, (image.height - size) / 2, size, size))
            }
            catch (e: FileNotFoundException) {
                if (i == quality.ordinal + 1) {
                    return Result.failure(e)
                }
            }
        }

        return Result.failure(IllegalStateException())
    }

    @Composable
    override fun PreviewSquare(params: PreviewParams) {
        SongPreviewSquare(this, params)
    }

    @Composable
    fun PreviewSquare(params: PreviewParams, queue_index: Int?) {
        SongPreviewSquare(this, params, queue_index = queue_index)
    }

    @Composable
    override fun PreviewLong(params: PreviewParams) {
        SongPreviewLong(this, params)
    }

    @Composable
    fun PreviewLong(params: PreviewParams, queue_index: Int?) {
        SongPreviewLong(this, params, queue_index = queue_index)
    }

    override val url: String get() = "https://music.youtube.com/watch?v=$id"
}