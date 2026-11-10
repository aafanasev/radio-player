package net.afanasev.radioplayer.core.player.fsm

import android.app.Application
import android.content.Context
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.afanasev.radioplayer.core.RadioPlayerHost

/**
 * App-scoped player state machine. It outlives any screen, so playback changes made from the
 * notification, Bluetooth or Android Auto are tracked even with no UI alive.
 *
 * Observe [state] and call [send]; get the instance with [RadioPlayer.get]. Events are
 * reduced one at a time on the main thread (Media3 requires it), which also serialises them.
 */
class RadioPlayer internal constructor(
    private val policy: PlayerPolicy,
    scope: CoroutineScope,
    effectsFactory: (send: (PlayerEvent) -> Unit) -> PlayerEffects,
) {

    private val _state = MutableStateFlow(PlayerState())

    /** Collecting this keeps a `MediaController` connected; see [PlayerEffects.setControllerNeeded]. */
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val events = Channel<PlayerEvent>(Channel.UNLIMITED)
    private val effects = effectsFactory(::send)

    /** Added to the ExoPlayer by [net.afanasev.radioplayer.core.player.PlaybackService]. */
    internal val playerListener: Player.Listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAYER_ERROR,
                )
            ) {
                // Report an error only in the batch that raised it, see PlayerSnapshot.error.
                val newError = player.playerError.takeIf { events.contains(Player.EVENT_PLAYER_ERROR) }
                send(PlayerEvent.PlayerChanged(player.toSnapshot(error = newError)))
            }
            if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
                send(PlayerEvent.MetadataChanged(player.mediaMetadata.toTrackMetadata()))
            }
        }
    }

    init {
        scope.launch {
            for (event in events) {
                val next = reduce(_state.value, event, policy)
                _state.value = next.state
                next.effects.forEach(effects::handle)
                updateControllerDemand()
            }
        }
        scope.launch {
            _state.subscriptionCount.collect { updateControllerDemand() }
        }
    }

    fun send(event: PlayerEvent) {
        events.trySend(event)
    }

    private fun updateControllerDemand() {
        effects.setControllerNeeded(_state.subscriptionCount.value > 0 || _state.value.playback.isActive)
    }

    companion object {
        @Volatile
        private var instance: RadioPlayer? = null

        /** Returns the app-wide instance, creating it from the [RadioPlayerHost] application. */
        fun get(context: Context): RadioPlayer =
            instance ?: synchronized(this) {
                instance ?: create(context.applicationContext as Application).also { instance = it }
            }

        private fun create(app: Application): RadioPlayer {
            val host = app as? RadioPlayerHost
                ?: error("${app.javaClass.name} must implement ${RadioPlayerHost::class.java.name}")
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            return RadioPlayer(
                policy = PlayerPolicy(streamUrl = host.radioPlayerConfig.streamUrl),
                scope = scope,
            ) { send ->
                PlayerEffectHandler(
                    context = app,
                    metadata = host.nowPlayingMetadataProvider,
                    analytics = host.playerAnalytics,
                    scope = scope,
                    send = send,
                )
            }
        }
    }
}
