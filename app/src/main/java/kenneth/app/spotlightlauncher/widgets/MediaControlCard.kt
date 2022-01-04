package kenneth.app.spotlightlauncher.widgets

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
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.MediaControlCardBinding
import kenneth.app.spotlightlauncher.utils.activity
import kenneth.app.spotlightlauncher.views.DateTimeViewContainer
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Displays a media control card on the home screen when media is playing.
 * Requires notification listener access in order to function properly.
 */
@AndroidEntryPoint
class MediaControlCard(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs), LifecycleObserver {
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var mediaSessionManager: MediaSessionManager

    /**
     * The ComponentName of the notification listener stub
     */
    private val notificationListenerStubComponent =
        ComponentName(context, NotificationListenerStub::class.java)

    private val playButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play)!!
    private val pauseButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause)!!

    /**
     * True when user seeks to another position. Used by the polling function to prevent
     * it from reverting the seek bar back to the old position.
     *
     * Since TransportControl.seekTo does not change the position immediately,
     * when the seek bar is moved by the user, the polling function needs to stop updating seek bar value
     * until position of actionMediaSession is updated, otherwise it'd immediately get back
     * the old position from activeMediaSession and it'd change the position of seek bar
     * to the old position.
     */
    private var newProgressSet = false

    private var activeMediaSession: MediaController? = null

    private val binding = MediaControlCardBinding.inflate(LayoutInflater.from(context), this, true)

    private val dateTimeViewContainer
        get() = parent as DateTimeViewContainer?

    /**
     * This thread is used to poll and show the current media progress, since there is no listeners
     * available to listen to media progress.
     */
    private val mediaProgressPollingScope = CoroutineScope(Dispatchers.Default)

    /**
     * Determines if this widget should poll media progress every second.
     */
    private var pollMediaProgress = false
        set(poll) {
            field = poll
            if (poll) {
                pollAndShowMediaProgress()
            }
        }

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
            metadata?.let { showMediaMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            state?.let { showPlaybackState(it) }
        }

        override fun onSessionDestroyed() {
            activeMediaSession?.unregisterCallback(this)
            activeMediaSession = null
        }
    }

    private val mediaSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            pollMediaProgress = false
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            newProgressSet = true
            pollMediaProgress = true
            seekBar?.let {
                activeMediaSession?.transportControls?.seekTo(it.progress.toLong())
            }
        }
    }

    init {
        if (isMediaControlEnabled) {
            checkNotificationListenerAndUpdate()
            attachListeners()
        } else {
            isVisible = false
            dateTimeViewContainer?.gravity = Gravity.CENTER
        }

        activity?.lifecycle?.addObserver(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        dateTimeViewContainer.apply {
            gravity =
                if (this@MediaControlCard.isVisible) Gravity.CENTER or Gravity.BOTTOM
                else Gravity.CENTER
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
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun checkNotificationListenerAndUpdate() {
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
        binding.playPauseButton.setOnClickListener { togglePlayPause() }
        binding.skipBackwardButton.setOnClickListener { skipBackward() }
        binding.skipForwardButton.setOnClickListener { skipForward() }
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
        binding.playPauseButton.disabled = true
        activeMediaSession?.transportControls?.skipToNext()
    }

    private fun skipBackward() {
        binding.playPauseButton.disabled = true
        activeMediaSession?.transportControls?.skipToPrevious()
    }

    /**
     * Hide media control. Can be one of the following reasons:
     * - there is no media currently playing
     * - notification listener is revoked manually by the user
     */
    private fun hideControl() {
        isVisible = false
        pollMediaProgress = false
        activeMediaSession?.unregisterCallback(activeMediaSessionListener)
        activeMediaSession = null
        dateTimeViewContainer?.gravity = Gravity.CENTER
//        binding.mediaControlBlurBackground.pauseBlur()
    }

    /**
     * Updates the instance of the currently active MediaController.
     * Also reflects the changes in the UI.
     */
    private fun updateActiveMediaSession(newSession: MediaController?) {
        activeMediaSession = newSession

        if (activeMediaSession != null) {
            showMediaControl()
            activeMediaSession?.registerCallback(activeMediaSessionListener)
            pollMediaProgress = true
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
        isInvisible = false
        dateTimeViewContainer?.gravity = Gravity.CENTER or Gravity.BOTTOM

        activeMediaSession?.let {
            it.metadata?.let(::showMediaMetadata)
            it.playbackState?.let(::showPlaybackState)
        }

//        binding.mediaControlBlurBackground.startBlur()
    }

    /**
     * Reflects the given MediaMetadata to the UI.
     */
    private fun showMediaMetadata(mediaMetadata: MediaMetadata) {
        binding.mediaTitle.text = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: context.getString(R.string.no_album_title_label)

        binding.mediaArtistName.text =
            mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: context.getString(R.string.no_artist_label)

        mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?.let {
                binding.mediaCover.isVisible = true
                Glide.with(context)
                    .load(it)
                    .into(binding.mediaCover)
            }
            ?: run {
                binding.mediaCover.isVisible = false
            }

        mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION).let {
            if (it != 0L) {
                binding.mediaSeekBar.apply {
                    isVisible = true
                    max = it.toInt()
                }.also { seekBar ->
                    seekBar.setOnSeekBarChangeListener(mediaSeekBarChangeListener)
                }
            }
        }
    }

    /**
     * Reflects the given PlaybackState to the UI.
     */
    private fun showPlaybackState(playbackState: PlaybackState) {
        when (playbackState.state) {
            PlaybackState.STATE_PLAYING -> {
                binding.playPauseButton.apply {
                    icon = pauseButtonDrawable
                    disabled = false
                }
            }
            PlaybackState.STATE_PAUSED -> {
                binding.playPauseButton.apply {
                    icon = playButtonDrawable
                    disabled = false
                }
            }
            else -> {
                binding.playPauseButton.apply {
                    icon = playButtonDrawable
                    disabled = true
                }
            }
        }

        binding.mediaSeekBar.apply {
            progress = playbackState.position.toInt()
            isEnabled = playbackState.actions and PlaybackState.ACTION_SEEK_TO != 0L
        }

        binding.skipBackwardButton.disabled =
            playbackState.actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS == 0L

        binding.skipForwardButton.disabled =
            playbackState.actions and PlaybackState.ACTION_SKIP_TO_NEXT == 0L
    }

    /**
     * Continuously poll and show currently media progress every second.
     */
    private fun pollAndShowMediaProgress() {
        mediaProgressPollingScope.launch {
            withContext(Dispatchers.Default) {
                while (pollMediaProgress) {
                    activeMediaSession?.playbackState?.position?.toInt()?.let {
                        activity?.runOnUiThread {
                            if (newProgressSet) {
                                if (it == binding.mediaSeekBar.progress) {
                                    newProgressSet = false
                                }
                            } else {
                                binding.mediaSeekBar.progress = it
                            }
                        }
                    }

                    withContext(Dispatchers.Default) {
                        delay(1000)
                    }
                }
            }
        }
    }
}

/**
 * A NotificationListenerService stub is required in order to register this app
 * as a notification listener, which enables access to the currently playing media.
 *
 * This app does NOT monitor or track device notifications.
 */
class NotificationListenerStub : NotificationListenerService()
