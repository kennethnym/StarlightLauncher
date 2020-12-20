package kenneth.app.spotlightlauncher.views

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import javax.inject.Inject

/**
 * Displays a media control card on the home screen when media is playing.
 * Requires notification listener access in order to function properly.
 */
@AndroidEntryPoint
class MediaControlCard(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var mediaSessionManager: MediaSessionManager

    /**
     * The ComponentName of the notification listener stub
     */
    private val notificationListenerStubComponent =
        ComponentName(context, NotificationListenerStub::class.java)

    private val playButtonDrawable = context.getDrawable(R.drawable.ic_play)!!
    private val pauseButtonDrawable = context.getDrawable(R.drawable.ic_pause)!!

    private var activeMediaSession: MediaController? = null

    private val mediaTitle: TextView
    private val mediaArtist: TextView
    private val mediaCover: ImageView
    private val skipBackwardButton: IconButton
    private val skipForwardButton: IconButton
    private val playPauseButton: IconButton
    private val blurBackground: BlurView

    /**
     * Gets from SharedPreferences whether media control is enabled by the user
     */
    private val isMediaControlEnabled: Boolean
        get() = sharedPreferences.getBoolean(
            context.getString(R.string.media_control_enabled), true
        )

    /**
     * Listens to changes to the current active media session.
     */
    private val activeMediaSessionListener = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            if (metadata != null) showMediaMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state != null) showPlaybackState(state)
        }

        override fun onSessionDestroyed() {
            activeMediaSession?.unregisterCallback(this)
            activeMediaSession = null
        }
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        inflate(context, R.layout.media_control_card, this)

        mediaTitle = findViewById(R.id.media_title)
        mediaArtist = findViewById(R.id.media_artist_name)
        mediaCover = findViewById(R.id.media_cover)
        skipBackwardButton = findViewById(R.id.skip_backward_button)
        skipForwardButton = findViewById(R.id.skip_forward_button)
        playPauseButton = findViewById(R.id.play_pause_button)
        blurBackground = findViewById(R.id.media_control_blur_background)

        if (isMediaControlEnabled) {
            checkNotificationListenerAndUpdate()
            attachListeners()
        } else {
            visibility = View.INVISIBLE
        }
    }

    /**
     * Check if notification listener is enabled for this app.
     * If it is enabled, then check if there is any media currently playing and updates the
     * UI accordingly. Otherwise, hide control.
     *
     * Call this method when the app resumes, because user may have revoked
     * notification listener for this app when the app is in background.
     */
    fun checkNotificationListenerAndUpdate() {
        if (isMediaControlEnabled && isNotificationListenerEnabled()) {
            val activeMediaSessions = mediaSessionManager.getActiveSessions(
                notificationListenerStubComponent
            )

            updateActiveMediaSession(
                if (activeMediaSessions.isNotEmpty())
                    activeMediaSessions.first()
                else null
            )

            addMediaSessionListener()
        } else {
            hideControl()
        }
    }

    private fun attachListeners() {
        playPauseButton.setOnClickListener { togglePlayPause() }
        skipBackwardButton.setOnClickListener { skipBackward() }
        skipForwardButton.setOnClickListener { skipForward() }
        sharedPreferences.registerOnSharedPreferenceChangeListener(::onSharedPreferenceChanged)
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == context.getString(R.string.media_control_enabled)) {
            val isMediaControlEnabled = sharedPreferences.getBoolean(key, false)

            if (isMediaControlEnabled) {
                checkNotificationListenerAndUpdate()
                attachListeners()
            } else {
                hideControl()
            }
        }
    }

    /**
     * Determines if the registered notification listener is enabled.
     * If true, then the currently playing media is accessible.
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val notificationListenerStr =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")

        return notificationListenerStr != null && notificationListenerStr.contains(context.packageName)
    }

    /**
     * Attaches a listener to MediaSessionManager that updates the instance of
     * the currently active MediaController when active session changes.
     */
    private fun addMediaSessionListener() {
        Log.i("", "add listener")
        mediaSessionManager.addOnActiveSessionsChangedListener(
            { sessions ->
                if (sessions != null && sessions.isNotEmpty()) {
                    updateActiveMediaSession(sessions.first())
                }
            },
            ComponentName(context, NotificationListenerStub::class.java)
        )
    }

    private fun skipForward() {
        playPauseButton.disabled = true
        activeMediaSession?.transportControls?.skipToNext()
    }

    private fun skipBackward() {
        playPauseButton.disabled = true
        activeMediaSession?.transportControls?.skipToPrevious()
    }

    /**
     * Hide media control. Can be one of the following reasons:
     * - there is no media currently playing
     * - notification listener is revoked manually by the user
     */
    private fun hideControl() {
        visibility = View.INVISIBLE
        activeMediaSession?.unregisterCallback(activeMediaSessionListener)
        activeMediaSession = null
        blurBackground.pauseBlur()
    }

    /**
     * Updates the instance of the currently active MediaController.
     * Also reflects the changes in the UI.
     */
    private fun updateActiveMediaSession(newSession: MediaController?) {
        Log.i("", "update")

        activeMediaSession = newSession

        if (activeMediaSession != null) {
            showMediaControl()
            activeMediaSession?.registerCallback(activeMediaSessionListener)
        } else {
            hideControl()
        }
    }

    /**
     * Toggles media play pause based on the current playback state
     */
    private fun togglePlayPause() {
        activeMediaSession?.let {
            when (it.playbackState?.state) {
                PlaybackState.STATE_PLAYING -> {
                    it.transportControls.pause()
                }
                PlaybackState.STATE_PAUSED -> {
                    it.transportControls.play()
                }
                else -> {
                }
            }
        }
    }

    /**
     * Shows media control and info for the currently playing media.
     */
    private fun showMediaControl() {
        visibility = View.VISIBLE

        activeMediaSession?.let {
            it.metadata?.let(::showMediaMetadata)
            it.playbackState?.let(::showPlaybackState)
        }

        blurBackground.startBlur()
    }

    /**
     * Reflects the given MediaMetadata to the UI.
     */
    private fun showMediaMetadata(mediaMetadata: MediaMetadata) {
        mediaTitle.text = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: context.getString(R.string.no_album_title_label)

        mediaArtist.text = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: context.getString(R.string.no_artist_label)

        mediaCover.apply {
            val coverBitmap = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)

            if (coverBitmap != null) {
                isVisible = true
                setImageBitmap(coverBitmap)
            } else {
                isVisible = false
            }
        }
    }

    /**
     * Reflects the given PlaybackState to the UI.
     */
    private fun showPlaybackState(playbackState: PlaybackState) {
        when (playbackState.state) {
            PlaybackState.STATE_PLAYING -> {
                playPauseButton.apply {
                    icon = pauseButtonDrawable
                    disabled = false
                }
            }
            PlaybackState.STATE_PAUSED -> {
                playPauseButton.apply {
                    icon = playButtonDrawable
                    disabled = false
                }
            }
            else -> {
                playPauseButton.apply {
                    icon = playButtonDrawable
                    disabled = true
                }
            }
        }

        skipBackwardButton.disabled =
            playbackState.actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS == 0L

        skipForwardButton.disabled =
            playbackState.actions and PlaybackState.ACTION_SKIP_TO_NEXT == 0L
    }
}

/**
 * A NotificationListenerService stub is required in order to register this app
 * as a notification listener, which enables access to the currently playing media.
 *
 * This app does NOT monitor or track device notifications.
 */
class NotificationListenerStub : NotificationListenerService()
