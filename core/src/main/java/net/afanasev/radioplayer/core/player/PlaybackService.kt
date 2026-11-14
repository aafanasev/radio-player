package net.afanasev.radioplayer.core.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.afanasev.radioplayer.core.RadioPlayerConfig
import net.afanasev.radioplayer.core.RadioPlayerHost
import net.afanasev.radioplayer.core.player.fsm.PlayerEvent
import net.afanasev.radioplayer.core.player.fsm.RadioPlayer

private const val ROOT_ID = "root"
private const val STATION_ID = "station"

/**
 * Configure via [RadioPlayerHost] on your [android.app.Application] and register this service
 * in your manifest (`android:foregroundServiceType="mediaPlayback"`, media3 session action).
 */
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val host by lazy { application as RadioPlayerHost }
    private val config: RadioPlayerConfig by lazy { host.radioPlayerConfig }
    private val radioPlayer by lazy { RadioPlayer.get(this) }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // stop playing music when become noisy (e.g. unplug headphones)
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                mediaLibrarySession?.let {
                    if (it.player.isPlaying) {
                        // pause() too, so playWhenReady is false and onTaskRemoved can stop us.
                        it.player.pause()
                        it.player.stop()
                    }
                }
            }
        }
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        @Suppress("WrongConstant") // RESULT_ERROR_BAD_VALUE is the stable alias for SessionError.ERROR_BAD_VALUE
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (parentId != ROOT_ID) {
                return Futures.immediateFuture(
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                )
            }
            val stationItem = MediaItem.Builder()
                .setMediaId(STATION_ID)
                .setUri(config.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(config.stationName)
                        .setStation(config.stationName)
                        .setArtworkUri(config.defaultArtworkUri.toUri())
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(stationItem), params))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        ContextCompat.registerReceiver(
            this,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        destroyMediaSession()
        unregisterReceiver(noisyReceiver)
        radioPlayer.send(PlayerEvent.ServiceDestroyed)
        super.onDestroy()
    }

    private fun initMediaSession() {
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true, /* handleAudioFocus */
            )
            .build()

        // Feeds every playback change, whoever caused it, into the app-scoped state machine.
        player.addListener(radioPlayer.playerListener)

        // The state machine decides the artwork; this mirrors it onto the session's media item for
        // the notification and lock screen, also onto a new item when playback (re)starts.
        serviceScope.launch {
            radioPlayer.artworkUri.collect { showArtwork(player) }
        }
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) showArtwork(player)
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, config.sessionActivityClass),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, libraryCallback)
            .setSessionActivity(sessionActivity)
            .build()
    }

    /** Replacing the item only when the artwork differs also stops this from looping on itself. */
    private fun showArtwork(player: Player) {
        val uri = (radioPlayer.artworkUri.value ?: config.defaultArtworkUri).toUri()
        val currentItem = player.currentMediaItem ?: return
        if (currentItem.mediaMetadata.artworkUri == uri) return
        val updatedMetadata = currentItem.mediaMetadata.buildUpon()
            .setArtworkUri(uri)
            .build()
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            currentItem.buildUpon().setMediaMetadata(updatedMetadata).build(),
        )
    }

    private fun destroyMediaSession() {
        serviceScope.cancel()
        mediaLibrarySession?.let {
            it.player.removeListener(radioPlayer.playerListener)
            it.player.release()
            it.release()
            mediaLibrarySession = null
        }
    }
}
