package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.model.radio.RadioState
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState

internal const val AUTO_DOWNLOAD_SOFT_TIMEOUT = 1500 // ms

data class PlayerServiceLoadState(
    val loading: Boolean,
    val loading_message: String? = null
)

interface PlayerService {
    val context: AppContext
    val service_player: PlayerServicePlayer

    fun onCreate()
    fun onDestroy()

    val state: SpMsPlayerState
    val is_playing: Boolean
    val song_count: Int
    val current_song_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val has_focus: Boolean

    val radio_instance: RadioInstance

    var repeat_mode: SpMsPlayerRepeatMode
    var volume: Float

    fun isPlayingOverLatentDevice(): Boolean

    fun play()
    fun pause()
    fun playPause()

    fun seekTo(position_ms: Long)
    fun seekToSong(index: Int)
    fun seekToNext()
    fun seekToPrevious()
    fun undoSeek()

    fun getSong(): Song?
    fun getSong(index: Int): Song?

    fun addSong(song: Song, index: Int)
    fun moveSong(from: Int, to: Int)
    fun removeSong(index: Int)

    fun addListener(listener: PlayerListener)
    fun removeListener(listener: PlayerListener)

    @Composable
    fun Visualiser(colour: Color, modifier: Modifier, opacity: Float)
}

expect class PlatformPlayerService: PlayerService {
    companion object {
        fun isServiceRunning(context: AppContext): Boolean

        fun addListener(listener: PlayerListener)
        fun removeListener(listener: PlayerListener)

        fun connect(
            context: AppContext,
            instance: PlatformPlayerService? = null,
            onConnected: (PlatformPlayerService) -> Unit,
            onDisconnected: () -> Unit
        ): Any

        fun disconnect(context: AppContext, connection: Any)
    }

    val load_state: PlayerServiceLoadState
    val connection_error: Throwable?

    override val context: AppContext
    override val service_player: PlayerServicePlayer

    override fun onCreate()
    override fun onDestroy()

    override val state: SpMsPlayerState
    override val is_playing: Boolean
    override val song_count: Int
    override val current_song_index: Int
    override val current_position_ms: Long
    override val duration_ms: Long
    override val has_focus: Boolean

    override val radio_instance: RadioInstance

    override var repeat_mode: SpMsPlayerRepeatMode
    override var volume: Float

    override fun isPlayingOverLatentDevice(): Boolean

    override fun play()
    override fun pause()
    override fun playPause()

    override fun seekTo(position_ms: Long)
    override fun seekToSong(index: Int)
    override fun seekToNext()
    override fun seekToPrevious()
    override fun undoSeek()

    override fun getSong(): Song?
    override fun getSong(index: Int): Song?

    override fun addSong(song: Song, index: Int)
    override fun moveSong(from: Int, to: Int)
    override fun removeSong(index: Int)

    override fun addListener(listener: PlayerListener)
    override fun removeListener(listener: PlayerListener)

    @Composable
    override fun Visualiser(colour: Color, modifier: Modifier, opacity: Float)
}
