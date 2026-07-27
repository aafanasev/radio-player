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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.afanasev.radioplayer.core.RadioPlayerConfig
import net.afanasev.radioplayer.core.RadioPlayerHost

private const val ROOT_ID = "root"
private const val STATION_ID = "station"

/**
 * Configure via [RadioPlayerHost] on your [android.app.Application] and register this service
 * in your manifest (`android:foregroundServiceType="mediaPlayback"`, media3 session action).
 */
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var artworkJob: Job? = null
    private var lastFetchedTitle: String? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val host by lazy { application as RadioPlayerHost }
    private val config: RadioPlayerConfig by lazy { host.radioPlayerConfig }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // stop playing music when become noisy (e.g. unplug headphones)
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                mediaLibrarySession?.let {
                    if (it.player.isPlaying) {
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

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE) {
                    lastFetchedTitle = null
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val title = mediaMetadata.title?.toString()
                if (title == null || title == lastFetchedTitle) return
                lastFetchedTitle = title

                artworkJob?.cancel()
                artworkJob = serviceScope.launch {
                    val uri = host.nowPlayingMetadataProvider.fetchArtworkUri(title)
                        ?: config.defaultArtworkUri
                    val currentItem = player.currentMediaItem ?: return@launch
                    val updatedMetadata = currentItem.mediaMetadata.buildUpon()
                        .setArtworkUri(uri.toUri())
                        .build()
                    player.replaceMediaItem(
                        player.currentMediaItemIndex,
                        currentItem.buildUpon().setMediaMetadata(updatedMetadata).build()
                    )
                }
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

    private fun destroyMediaSession() {
        artworkJob?.cancel()
        serviceScope.cancel()
        mediaLibrarySession?.let {
            it.player.release()
            it.release()
            mediaLibrarySession = null
        }
    }
}
