package net.afanasev.radioplayer.core.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.afanasev.radioplayer.core.RadioPlayerHost
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent
import net.afanasev.radioplayer.core.player.fsm.PlayerState
import net.afanasev.radioplayer.core.player.fsm.RadioPlayer

/** Thin adapter over the app-scoped [RadioPlayer]. */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val player = RadioPlayer.get(application)

    val defaultArtworkUri: String = (application as RadioPlayerHost).radioPlayerConfig.defaultArtworkUri

    val state: StateFlow<PlayerState> = player.state

    fun onPlayPause() = player.send(PlayerEvent.PlayPauseClicked)

    fun onRetry() = player.send(PlayerEvent.RetryClicked)

    @Deprecated("Use onPlayPause()", ReplaceWith("onPlayPause()"))
    fun playPause() = onPlayPause()

    @Deprecated("Use state.nowPlaying.artworkUri ?: defaultArtworkUri")
    val artworkUri: StateFlow<String> =
        derive(defaultArtworkUri) { it.nowPlaying.artworkUri ?: defaultArtworkUri }

    @Deprecated("Use state.nowPlaying.title")
    val title: StateFlow<String> = derive("") { it.nowPlaying.title }

    @Deprecated("Use state.nowPlaying.nextTrackTitle")
    val nextTrackTitle: StateFlow<String> = derive("") { it.nowPlaying.nextTrackTitle }

    @Deprecated("Use state.playback.toButtonState()")
    val buttonState: StateFlow<PlayerButtonState> =
        derive(PlayerButtonState.LOADING) { it.playback.toButtonState() }

    private fun <T> derive(initial: T, transform: (PlayerState) -> T): StateFlow<T> =
        player.state.map(transform).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}
