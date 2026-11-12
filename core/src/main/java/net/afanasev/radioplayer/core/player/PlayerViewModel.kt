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

/**
 * Adapts the app-scoped [RadioPlayer] state machine to UI flows. All playback logic lives in
 * the state machine; this only maps its state and forwards clicks.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val player = RadioPlayer.get(application)
    private val defaultArtworkUri = (application as RadioPlayerHost).radioPlayerConfig.defaultArtworkUri

    val artworkUri: StateFlow<String> = derive { it.nowPlaying.artworkUri ?: defaultArtworkUri }

    val title: StateFlow<String> = derive { it.nowPlaying.title }

    val nextTrackTitle: StateFlow<String> = derive { it.nowPlaying.nextTrackTitle }

    val buttonState: StateFlow<PlayerButtonState> = derive { it.playback.toButtonState() }

    fun playPause() = player.send(PlayerEvent.PlayPauseClicked)

    /**
     * Starts from the player's current state, so returning to the screen shows no empty frame.
     * While collected, the player keeps its `MediaController` connected; 5 s after the UI stops
     * collecting it's released (unless playing), so the service can shut down.
     */
    private fun <T> derive(transform: (PlayerState) -> T): StateFlow<T> =
        player.state
            .map(transform)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), transform(player.state.value))
}
