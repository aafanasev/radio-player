package net.afanasev.radioplayer.core.player.fsm

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import net.afanasev.radioplayer.core.player.PlayerButtonState
import net.afanasev.radioplayer.core.player.fsm.AnalyticsEvent.PlayButtonClick
import net.afanasev.radioplayer.core.player.fsm.AnalyticsEvent.PlaybackError
import net.afanasev.radioplayer.core.player.fsm.AnalyticsEvent.Reconnect
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.CancelConnectTimeout
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.CancelRetry
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.FetchArtwork
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.FetchNextTrack
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.ReleaseController
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.ScheduleRetry
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.StartConnectTimeout
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.StartPlayback
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.StopForRetry
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.StopPlayback
import net.afanasev.radioplayer.core.player.fsm.PlayerEffect.TrackAnalytics
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.ConnectTimedOut
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.ControllerConnected
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.ControllerFailed
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.MetadataChanged
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.ArtworkLoaded
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.NextTrackLoaded
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.PlayPauseClicked
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.PlayerChanged
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.RetryDue
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent.ServiceDestroyed
import net.afanasev.radioplayer.core.player.toButtonState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val URL = "https://stream.example.com/live"

class PlayerReducerTest {

    private val policy = PlayerPolicy(
        streamUrl = URL,
        connectTimeoutMs = 10_000,
        maxReconnectAttempts = 3,
        retryBaseDelayMs = 1_000,
        retryMaxDelayMs = 3_000,
    )

    private val activeStates = listOf(
        Playback.Connecting,
        Playback.Playing,
        Playback.Buffering,
        Playback.Reconnecting(1),
    )

    private fun state(playback: Playback, title: String = "Song A") =
        PlayerState(playback, NowPlaying(title = title, artworkUri = "art://a", nextTrackTitle = "Song B"))

    private fun reduce(from: PlayerState, event: PlayerEvent) = reduce(from, event, policy)

    private fun snapshot(
        playWhenReady: Boolean = true,
        playbackState: Int = Player.STATE_READY,
        isPlaying: Boolean = false,
        error: PlaybackException? = null,
    ) = PlayerSnapshot(playWhenReady, playbackState, isPlaying, error)

    private val playing = snapshot(isPlaying = true)
    private val stalled = snapshot(playbackState = Player.STATE_BUFFERING)
    private val paused = snapshot(playWhenReady = false)
    private val stopped = snapshot(playbackState = Player.STATE_IDLE)
    private val ended = snapshot(playbackState = Player.STATE_ENDED)

    private fun error(code: Int) = PlaybackException("boom", null, code)
    private val networkError = error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
    private val sourceError = error(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)

    private fun assertUnchanged(from: PlayerState, event: PlayerEvent) {
        assertEquals(Next(from), reduce(from, event))
    }

    // --- Disconnected ---------------------------------------------------------------------

    @Test
    fun `Disconnected + ControllerConnected playing goes to Playing and fetches artwork and next track`() {
        val next = reduce(PlayerState(), ControllerConnected(playing, TrackMetadata("Song A")))
        assertEquals(Playback.Playing, next.state.playback)
        assertEquals(NowPlaying("Song A", null, ""), next.state.nowPlaying)
        assertEquals(listOf(FetchArtwork("Song A"), FetchNextTrack("Song A")), next.effects)
    }

    @Test
    fun `Disconnected + ControllerConnected not playing goes to Idle`() {
        val next = reduce(PlayerState(), ControllerConnected(stopped, TrackMetadata(null)))
        assertEquals(PlayerState(Playback.Idle), next.state)
        assertEquals(emptyList<PlayerEffect>(), next.effects)
    }

    @Test
    fun `Disconnected + ControllerFailed goes to Error CONTROLLER`() {
        val cause = IllegalStateException()
        val next = reduce(state(Playback.Disconnected), ControllerFailed(cause))
        assertEquals(Playback.Error(ErrorReason.CONTROLLER), next.state.playback)
        assertTrue(TrackAnalytics(PlaybackError(ErrorReason.CONTROLLER, cause)) in next.effects)
    }

    @Test
    fun `Disconnected + PlayPauseClicked starts, connecting on demand`() {
        // The button shows LOADING for both Disconnected and connecting, so a tap must work.
        val next = reduce(state(Playback.Disconnected), PlayPauseClicked)
        assertEquals(state(Playback.Connecting), next.state)
        assertEquals(
            listOf(StartPlayback(URL), StartConnectTimeout(10_000), TrackAnalytics(PlayButtonClick(Playback.Disconnected))),
            next.effects,
        )
    }

    @Test
    fun `Disconnected ignores idle players`() {
        assertUnchanged(state(Playback.Disconnected), PlayerChanged(stopped))
    }

    @Test
    fun `Disconnected follows a player started with no UI alive`() {
        // e.g. Android Auto on a fresh process: no ControllerConnected will come on its own.
        assertEquals(Next(state(Playback.Playing)), reduce(state(Playback.Disconnected), PlayerChanged(playing)))
        assertEquals(Next(state(Playback.Buffering)), reduce(state(Playback.Disconnected), PlayerChanged(stalled)))
    }

    @Test
    fun `network error after a start with no UI alive is retried`() {
        val buffering = reduce(state(Playback.Disconnected), PlayerChanged(stalled)).state
        val next = reduce(buffering, PlayerChanged(snapshot(playbackState = Player.STATE_IDLE, error = networkError)))
        assertEquals(Playback.Reconnecting(1), next.state.playback)
    }

    // --- Starting ---------------------------------------------------------------------------

    @Test
    fun `Idle or Error + PlayPauseClicked goes to Connecting`() {
        // From Error this is the retry.
        for (from in listOf(Playback.Idle, Playback.Error(ErrorReason.NETWORK))) {
            val next = reduce(state(from), PlayPauseClicked)
            assertEquals(state(Playback.Connecting), next.state)
            assertEquals(
                listOf(StartPlayback(URL), StartConnectTimeout(10_000), TrackAnalytics(PlayButtonClick(from))),
                next.effects,
            )
        }
    }

    // --- Stopping ---------------------------------------------------------------------------

    @Test
    fun `loading states + PlayPauseClicked cancel to Idle`() {
        for (from in listOf(Playback.Connecting, Playback.Buffering, Playback.Reconnecting(2))) {
            val next = reduce(state(from), PlayPauseClicked)
            assertEquals(state(Playback.Idle), next.state)
            assertEquals(
                listOf(StopPlayback, CancelConnectTimeout, CancelRetry, TrackAnalytics(PlayButtonClick(from))),
                next.effects,
            )
        }
    }

    @Test
    fun `Playing + PlayPauseClicked goes to Idle and keeps now playing`() {
        val next = reduce(state(Playback.Playing), PlayPauseClicked)
        assertEquals(state(Playback.Idle), next.state)
        assertEquals(listOf(StopPlayback, TrackAnalytics(PlayButtonClick(Playback.Playing))), next.effects)
    }

    // --- Player changes ---------------------------------------------------------------------

    @Test
    fun `Connecting or Reconnecting + isPlaying goes to Playing`() {
        for (from in listOf(Playback.Connecting, Playback.Reconnecting(2))) {
            val next = reduce(state(from), PlayerChanged(playing))
            assertEquals(Playback.Playing, next.state.playback)
            assertEquals(listOf(CancelConnectTimeout, CancelRetry), next.effects)
        }
    }

    @Test
    fun `Connecting ignores intermediate snapshots of its own start`() {
        // stop, setMediaItem, play and prepare each produce their own onEvents batch.
        for (s in listOf(snapshot(false, Player.STATE_IDLE), stopped, stalled)) {
            assertUnchanged(state(Playback.Connecting), PlayerChanged(s))
        }
    }

    @Test
    fun `Connecting paused from outside goes to Idle and cancels the timeout`() {
        for (s in listOf(snapshot(false, Player.STATE_BUFFERING), paused)) {
            val next = reduce(state(Playback.Connecting), PlayerChanged(s))
            assertEquals(Next(state(Playback.Idle), listOf(CancelConnectTimeout, CancelRetry)), next)
        }
    }

    @Test
    fun `Reconnecting paused from outside goes to Idle and cancels the retry`() {
        val next = reduce(state(Playback.Reconnecting(2)), PlayerChanged(snapshot(false, Player.STATE_IDLE)))
        assertEquals(Next(state(Playback.Idle), listOf(CancelConnectTimeout, CancelRetry)), next)
    }

    @Test
    fun `Reconnecting ignores snapshots of its own retry`() {
        for (s in listOf(stopped, stalled)) assertUnchanged(state(Playback.Reconnecting(2)), PlayerChanged(s))
    }

    @Test
    fun `Playing + stalled goes to Buffering, and back to Playing`() {
        val buffering = reduce(state(Playback.Playing), PlayerChanged(stalled))
        assertEquals(Next(state(Playback.Buffering)), buffering)
        assertEquals(Next(state(Playback.Playing)), reduce(buffering.state, PlayerChanged(playing)))
    }

    @Test
    fun `network error in an active state goes to Reconnecting n+1`() {
        for (from in activeStates) {
            val n = (from as? Playback.Reconnecting)?.attempt ?: 0
            val next = reduce(state(from), PlayerChanged(snapshot(playbackState = Player.STATE_IDLE, error = networkError)))
            val delay = policy.retryDelayMs(n + 1)
            assertEquals(state(Playback.Reconnecting(n + 1)), next.state)
            assertEquals(
                listOf(CancelConnectTimeout, ScheduleRetry(n + 1, delay), TrackAnalytics(Reconnect(n + 1, delay))),
                next.effects,
            )
        }
    }

    @Test
    fun `network error after max attempts goes to Error NETWORK`() {
        val next = reduce(state(Playback.Reconnecting(3)), PlayerChanged(snapshot(error = networkError)))
        assertEquals(state(Playback.Error(ErrorReason.NETWORK)), next.state)
        assertEquals(
            listOf(CancelConnectTimeout, CancelRetry, TrackAnalytics(PlaybackError(ErrorReason.NETWORK, networkError))),
            next.effects,
        )
    }

    @Test
    fun `other error in an active state goes to Error SOURCE`() {
        for (from in activeStates) {
            val next = reduce(state(from), PlayerChanged(snapshot(error = sourceError)))
            assertEquals(state(Playback.Error(ErrorReason.SOURCE)), next.state)
            assertEquals(
                listOf(CancelConnectTimeout, CancelRetry, TrackAnalytics(PlaybackError(ErrorReason.SOURCE, sourceError))),
                next.effects,
            )
        }
    }

    @Test
    fun `errors outside active states are ignored`() {
        for (from in listOf(Playback.Disconnected, Playback.Idle, Playback.Error(ErrorReason.SOURCE))) {
            assertUnchanged(state(from), PlayerChanged(snapshot(error = networkError)))
        }
    }

    @Test
    fun `Playing or Buffering stopped from outside goes to Idle`() {
        for (from in listOf(Playback.Playing, Playback.Buffering)) {
            for (s in listOf(paused, stopped)) {
                assertEquals(Next(state(Playback.Idle)), reduce(state(from), PlayerChanged(s)))
            }
        }
    }

    @Test
    fun `Playing or Buffering + ENDED reconnects`() {
        for (from in listOf(Playback.Playing, Playback.Buffering)) {
            assertEquals(Playback.Reconnecting(1), reduce(state(from), PlayerChanged(ended)).state.playback)
        }
    }

    @Test
    fun `Idle or Error started from outside follows the player`() {
        for (from in listOf(Playback.Idle, Playback.Error(ErrorReason.NETWORK))) {
            assertEquals(Next(state(Playback.Playing)), reduce(state(from), PlayerChanged(playing)))
            assertEquals(Next(state(Playback.Buffering)), reduce(state(from), PlayerChanged(stalled)))
            assertUnchanged(state(from), PlayerChanged(stopped))
        }
    }

    // --- Timers -----------------------------------------------------------------------------

    @Test
    fun `Connecting + ConnectTimedOut stops, keeping playWhenReady, and goes to Reconnecting 1`() {
        val next = reduce(state(Playback.Connecting), ConnectTimedOut)
        assertEquals(state(Playback.Reconnecting(1)), next.state)
        assertEquals(
            listOf(StopForRetry, CancelConnectTimeout, ScheduleRetry(1, 1_000), TrackAnalytics(Reconnect(1, 1_000))),
            next.effects,
        )
    }

    @Test
    fun `ConnectTimedOut with no attempts left goes to Error NETWORK`() {
        val next = reduce(state(Playback.Connecting), ConnectTimedOut, policy.copy(maxReconnectAttempts = 0))
        assertEquals(state(Playback.Error(ErrorReason.NETWORK)), next.state)
        assertEquals(StopPlayback, next.effects.first())
    }

    @Test
    fun `Reconnecting + ConnectTimedOut moves to the next attempt`() {
        assertEquals(Playback.Reconnecting(3), reduce(state(Playback.Reconnecting(2)), ConnectTimedOut).state.playback)
    }

    @Test
    fun `ConnectTimedOut is ignored when not connecting`() {
        for (from in listOf(Playback.Idle, Playback.Playing, Playback.Buffering)) assertUnchanged(state(from), ConnectTimedOut)
    }

    @Test
    fun `Reconnecting n + RetryDue n restarts playback`() {
        val next = reduce(state(Playback.Reconnecting(2)), RetryDue(2))
        assertEquals(state(Playback.Reconnecting(2)), next.state)
        assertEquals(listOf(StartPlayback(URL), StartConnectTimeout(10_000)), next.effects)
    }

    @Test
    fun `stale RetryDue is ignored`() {
        assertUnchanged(state(Playback.Reconnecting(2)), RetryDue(1))
        assertUnchanged(state(Playback.Idle), RetryDue(1))
    }

    @Test
    fun `backoff doubles and is capped`() {
        assertEquals(listOf(1_000L, 2_000L, 3_000L, 3_000L), (1..4).map(policy::retryDelayMs))
    }

    // --- Now playing ------------------------------------------------------------------------

    @Test
    fun `MetadataChanged with a new title resets next track, keeps old artwork and fetches both, in any state`() {
        for (from in activeStates + Playback.Idle + Playback.Disconnected) {
            val next = reduce(state(from), MetadataChanged(TrackMetadata("Song C")))
            assertEquals(PlayerState(from, NowPlaying("Song C", "art://a", "")), next.state)
            assertEquals(listOf(FetchArtwork("Song C"), FetchNextTrack("Song C")), next.effects)
        }
    }

    @Test
    fun `MetadataChanged with the same title changes nothing`() {
        assertUnchanged(state(Playback.Playing), MetadataChanged(TrackMetadata("Song A")))
    }

    @Test
    fun `MetadataChanged without a title keeps now playing`() {
        assertUnchanged(state(Playback.Connecting), MetadataChanged(TrackMetadata(null)))
    }

    @Test
    fun `ArtworkLoaded for the current title sets it`() {
        val next = reduce(state(Playback.Playing), ArtworkLoaded("Song A", "art://new"))
        assertEquals(NowPlaying("Song A", "art://new", "Song B"), next.state.nowPlaying)
        assertEquals(emptyList<PlayerEffect>(), next.effects)
    }

    @Test
    fun `ArtworkLoaded without artwork falls back to the default`() {
        val next = reduce(state(Playback.Playing), ArtworkLoaded("Song A", null))
        assertEquals(null, next.state.nowPlaying.artworkUri)
    }

    @Test
    fun `ArtworkLoaded for an older title is discarded`() {
        assertUnchanged(state(Playback.Playing), ArtworkLoaded("Old song", "art://old"))
    }

    @Test
    fun `NextTrackLoaded for the current title sets it`() {
        val next = reduce(state(Playback.Playing), NextTrackLoaded("Song A", "Song Z"))
        assertEquals("Song Z", next.state.nowPlaying.nextTrackTitle)
    }

    @Test
    fun `NextTrackLoaded for an older title is discarded`() {
        assertUnchanged(state(Playback.Playing), NextTrackLoaded("Old song", "Song Z"))
    }

    // --- Service / controller lifecycle -----------------------------------------------------

    @Test
    fun `ServiceDestroyed goes to Disconnected from any state and keeps now playing`() {
        for (from in activeStates + Playback.Idle + Playback.Error(ErrorReason.SOURCE)) {
            val next = reduce(state(from), ServiceDestroyed)
            assertEquals(state(Playback.Disconnected), next.state)
            assertEquals(listOf(CancelConnectTimeout, CancelRetry, ReleaseController), next.effects)
        }
    }

    @Test
    fun `ControllerConnected while active only updates now playing`() {
        val next = reduce(state(Playback.Connecting), ControllerConnected(stopped, TrackMetadata("Song C")))
        assertEquals(Playback.Connecting, next.state.playback)
        assertEquals("Song C", next.state.nowPlaying.title)
    }

    @Test
    fun `ControllerFailed while connecting goes to Error CONTROLLER, otherwise only reports`() {
        val cause = IllegalStateException()
        assertEquals(
            Playback.Error(ErrorReason.CONTROLLER),
            reduce(state(Playback.Connecting), ControllerFailed(cause)).state.playback,
        )
        val idle = reduce(state(Playback.Idle), ControllerFailed(cause))
        assertEquals(state(Playback.Idle), idle.state)
        assertEquals(listOf(TrackAnalytics(PlaybackError(ErrorReason.CONTROLLER, cause))), idle.effects)
    }

    // --- Error classification ---------------------------------------------------------------

    @Test
    fun `error codes map to reasons`() {
        assertEquals(ErrorReason.NETWORK, networkError.toErrorReason())
        assertEquals(ErrorReason.NETWORK, error(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS).toErrorReason())
        assertEquals(ErrorReason.NETWORK, error(PlaybackException.ERROR_CODE_TIMEOUT).toErrorReason())
        assertEquals(ErrorReason.SOURCE, error(PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED).toErrorReason())
        assertEquals(ErrorReason.SOURCE, sourceError.toErrorReason())
        assertEquals(ErrorReason.UNKNOWN, error(PlaybackException.ERROR_CODE_UNSPECIFIED).toErrorReason())
    }

    @Test
    fun `button mapping`() {
        assertEquals(PlayerButtonState.LOADING, Playback.Disconnected.toButtonState())
        assertEquals(PlayerButtonState.LOADING, Playback.Connecting.toButtonState())
        assertEquals(PlayerButtonState.LOADING, Playback.Buffering.toButtonState())
        assertEquals(PlayerButtonState.LOADING, Playback.Reconnecting(1).toButtonState())
        assertEquals(PlayerButtonState.PAUSED, Playback.Idle.toButtonState())
        assertEquals(PlayerButtonState.PAUSED, Playback.Error(ErrorReason.NETWORK).toButtonState())
        assertEquals(PlayerButtonState.PLAYING, Playback.Playing.toButtonState())
    }
}
