package net.afanasev.radioplayer.core.player

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import net.afanasev.radioplayer.core.RadioPlayerHost

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val host = application as RadioPlayerHost
    private val config = host.radioPlayerConfig

    private val _artworkUri = MutableStateFlow(config.defaultArtworkUri)
    val artworkUri: StateFlow<String> = _artworkUri.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _nextTrackTitle = MutableStateFlow("")
    val nextTrackTitle: StateFlow<String> = _nextTrackTitle.asStateFlow()

    private val _buttonState = MutableStateFlow(PlayerButtonState.PAUSED)
    val buttonState: StateFlow<PlayerButtonState> = _buttonState.asStateFlow()

    private var mediaController: MediaController? = null

    init {
        viewModelScope.launch {
            val token =
                SessionToken(application, ComponentName(application, PlaybackService::class.java))

            mediaController =
                MediaController.Builder(application, token).buildAsync().await().also {
                    if (it.isPlaying) {
                        _buttonState.value = PlayerButtonState.PLAYING
                        setMetadata(it.mediaMetadata)
                    }

                    it.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            _buttonState.value =
                                if (playing) PlayerButtonState.PLAYING else PlayerButtonState.PAUSED
                        }

                        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                            setMetadata(mediaMetadata)
                        }
                    })
                }
        }
    }

    fun playPause() {
        host.playerAnalytics.onPlayButtonClick(_buttonState.value)

        if (_buttonState.value == PlayerButtonState.PAUSED) {
            _buttonState.value = PlayerButtonState.LOADING
        }

        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                val mediaItem = MediaItem.fromUri(config.streamUrl)
                it.setMediaItem(mediaItem)
                it.prepare()
                it.play()
            }
        }
    }

    private fun setMetadata(mediaMetadata: MediaMetadata) {
        _title.value = mediaMetadata.title?.toString() ?: ""
        _artworkUri.value = mediaMetadata.artworkUri?.toString() ?: config.defaultArtworkUri

        viewModelScope.launch {
            _nextTrackTitle.value = host.nowPlayingMetadataProvider.fetchNextTrack().orEmpty()
        }
    }

    override fun onCleared() {
        mediaController?.release()
        mediaController = null
    }
}
