package net.afanasev.radioplayer.core.player.fsm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RadioPlayerTest {

    private class FakeEffects : PlayerEffects {
        val handled = mutableListOf<PlayerEffect>()
        val demand = mutableListOf<Boolean>()
        override fun handle(effect: PlayerEffect) { handled += effect }
        override fun setControllerNeeded(needed: Boolean) { demand += needed }
    }

    private fun TestScope.player(effects: FakeEffects): RadioPlayer {
        val playerScope = TestScope(StandardTestDispatcher(testScheduler))
        return RadioPlayer(PlayerPolicy(streamUrl = "url"), playerScope.backgroundScope) { effects }
    }

    @Test
    fun `events are reduced in order and their effects run`() = runTest {
        val effects = FakeEffects()
        val player = player(effects)

        player.send(PlayerEvent.ControllerConnected(PlayerSnapshot(false, 1, false), TrackMetadata("A")))
        player.send(PlayerEvent.PlayPauseClicked)
        runCurrent()

        assertEquals(Playback.Connecting, player.state.value.playback)
        assertTrue(PlayerEffect.FetchNextTrack("A") in effects.handled)
        assertTrue(PlayerEffect.StartPlayback("url") in effects.handled)
    }

    @Test
    fun `controller is needed while observed or active`() = runTest {
        val effects = FakeEffects()
        val player = player(effects)
        runCurrent()
        assertEquals(false, effects.demand.last())

        val observer = launch { player.state.first { it.playback == Playback.Playing } }
        runCurrent()
        assertEquals(true, effects.demand.last())

        player.send(PlayerEvent.ControllerConnected(PlayerSnapshot(true, 3, true), TrackMetadata(null)))
        runCurrent()
        observer.join() // saw Playing and stopped observing
        runCurrent()
        assertEquals("still active, so still needed", true, effects.demand.last())

        player.send(PlayerEvent.PlayPauseClicked)
        runCurrent()
        assertEquals(Playback.Idle, player.state.value.playback)
        assertEquals(false, effects.demand.last())
    }
}
