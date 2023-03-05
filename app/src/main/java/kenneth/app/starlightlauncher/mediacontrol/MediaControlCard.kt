package kenneth.app.starlightlauncher.mediacontrol

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleObserver
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.databinding.MediaControlCardBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Displays a media control card on the home screen when media is playing.
 * Requires notification listener access in order to function properly.
 */
@AndroidEntryPoint
internal class MediaControlCard(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs), LifecycleObserver {
    @Inject
    lateinit var blurHandler: BlurHandler

    var mediaSession: MediaController? = null
        set(value) {
            field?.unregisterCallback(mediaSessionListener)
            field = value?.also {
                it.registerCallback(mediaSessionListener)
                pollMediaProgress = true
            }

            value?.metadata?.let { showMediaMetadata(it) }
            value?.playbackState?.let { showPlaybackState(it) }
        }

    private val playButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play)!!
    private val pauseButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause)!!

    private var lifecycleScope: LifecycleCoroutineScope? = null

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

    private val binding = MediaControlCardBinding.inflate(LayoutInflater.from(context), this, true)

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
     * Listens to changes to the current active media session.
     */
    private val mediaSessionListener = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata?.let { showMediaMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            state?.let { showPlaybackState(it) }
        }

        override fun onSessionDestroyed() {
            mediaSession?.unregisterCallback(this)
            mediaSession = null
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
                mediaSession?.transportControls?.seekTo(it.progress.toLong())
            }
        }
    }

    init {
        with(binding) {
            playPauseButton.setOnClickListener { togglePlayPause() }
            skipBackwardButton.setOnClickListener { skipBackward() }
            skipForwardButton.setOnClickListener { skipForward() }
            mediaControlBlurBackground.blurWith(blurHandler)
        }
    }

    fun setLifecycleScope(scope: LifecycleCoroutineScope) {
        lifecycleScope = scope
    }

    override fun onDetachedFromWindow() {
        mediaSession?.unregisterCallback(mediaSessionListener)
        super.onDetachedFromWindow()
    }

    private fun skipForward() {
        binding.playPauseButton.isEnabled = false
        mediaSession?.transportControls?.skipToNext()
    }

    private fun skipBackward() {
        binding.playPauseButton.isEnabled = false
        mediaSession?.transportControls?.skipToPrevious()
    }

    /**
     * Toggles media play pause based on the current playback state
     */
    private fun togglePlayPause() {
        mediaSession?.let {
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
     * Reflects the given MediaMetadata to the UI.
     */
    private fun showMediaMetadata(mediaMetadata: MediaMetadata) {
        binding.mediaTitle.text = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: context.getString(R.string.no_album_title_label)

        binding.mediaArtistName.text =
            mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: context.getString(R.string.no_artist_label)

        mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?.let {
                binding.mediaCover.isVisible = true
                Glide.with(context)
                    .load(it)
                    .into(binding.mediaCover)
            }
            ?: run {
                binding.mediaCover.isVisible = false
            }

//        mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
//            ?.let {
//                binding.mediaCover.isVisible = true
//                Glide.with(context)
//                    .load(it)
//                    .into(binding.mediaCover)
//            }
//            ?: run {
//                binding.mediaCover.isVisible = false
//            }

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
                    isEnabled = true
                }
            }
            PlaybackState.STATE_PAUSED -> {
                binding.playPauseButton.apply {
                    icon = playButtonDrawable
                    isEnabled = true
                }
            }
            else -> {
                binding.playPauseButton.apply {
                    icon = playButtonDrawable
                    isEnabled = false
                }
            }
        }

        binding.mediaSeekBar.apply {
            progress = playbackState.position.toInt()
            isEnabled = playbackState.actions and PlaybackState.ACTION_SEEK_TO != 0L
        }

        binding.skipBackwardButton.isEnabled =
            playbackState.actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L

        binding.skipForwardButton.isEnabled =
            playbackState.actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
    }

    /**
     * Continuously poll and show currently media progress every second.
     */
    private fun pollAndShowMediaProgress() {
        lifecycleScope?.launch {
            while (pollMediaProgress) {
                mediaSession?.playbackState?.position?.toInt()?.let {
                    if (newProgressSet) {
                        if (it == binding.mediaSeekBar.progress) {
                            newProgressSet = false
                        }
                    } else {
                        binding.mediaSeekBar.progress = it
                    }
                }
                delay(1000)
            }
        }
    }
}
