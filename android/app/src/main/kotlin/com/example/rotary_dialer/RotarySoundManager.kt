package com.example.rotary_dialer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.util.Log

/**
 * Manages sound effects for the rotary dial
 * Plays tick sounds during rotation and clack sound on release
 */
class RotarySoundManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RotarySoundManager"
        private const val MAX_STREAMS = 5
    }
    
    private var soundPool: SoundPool? = null
    private var tickSoundId: Int = 0
    private var clackSoundId: Int = 0
    private var dialSoundId: Int = 0
    private var isLoaded = false
    private var isDialSoundPlaying = false
    
    // Drag loop functionality
    private var dragStreamId: Int = 0
    
    // MediaPlayer for rotary dial sound (alternative approach)
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var toneGenerator: ToneGenerator? = null
    
    init {
        init(context)
        initializeSoundPool()
        loadSounds()
        initializeMediaPlayer()
    }
    
    fun init(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d(TAG, "AudioManager initialized")
    }
    
    private fun initializeSoundPool() {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            
            SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(2, android.media.AudioManager.STREAM_MUSIC, 0)
        }
        
        soundPool?.setOnLoadCompleteListener { soundPool, sampleId, status ->
            Log.d(TAG, "OnLoadComplete: sampleId=$sampleId, status=$status")
            if (status == 0) {
                // Only set loaded when the specific dial sound is ready
                if (sampleId == dialSoundId) {
                    isLoaded = true
                    Log.d(TAG, "Dial sound ready for playback - sampleId: $sampleId")
                }
            } else {
                Log.e(TAG, "Failed to load sound - sampleId: $sampleId, status: $status")
            }
        }
    }
    
    private fun loadSounds() {
        try {
            // Load the real rotary dial sound
            val dialResId = context.resources.getIdentifier("rotary_dial_full", "raw", context.packageName)
            Log.d(TAG, "Package name: ${context.packageName}")
            Log.d(TAG, "Dial resource ID: $dialResId")
            
            if (dialResId != 0) {
                dialSoundId = soundPool?.load(context, dialResId, 1) ?: 0
                Log.d(TAG, "Loading dial sound with ID: $dialSoundId (will be ready when OnLoadComplete fires)")
                
                // Don't set isLoaded immediately - wait for OnLoadCompleteListener
            } else {
                Log.e(TAG, "Could not find rotary_dial_full in raw resources!")
                
                // Try alternative approach - check if file exists
                try {
                    val inputStream = context.resources.openRawResource(
                        context.resources.getIdentifier("rotary_dial_full", "raw", context.packageName)
                    )
                    inputStream.close()
                    Log.d(TAG, "Raw resource file exists but getIdentifier failed")
                } catch (e: Exception) {
                    Log.e(TAG, "Raw resource file does not exist: $e")
                }
            }
            
            // Note: tick and clack sounds are disabled until custom sound files are added
            // val tickResId = context.resources.getIdentifier("tick", "raw", context.packageName)
            // val clackResId = context.resources.getIdentifier("clack", "raw", context.packageName)
            // if (tickResId != 0) tickSoundId = soundPool?.load(context, tickResId, 1) ?: 0
            // if (clackResId != 0) clackSoundId = soundPool?.load(context, clackResId, 1) ?: 0
            
            Log.d(TAG, "Sound manager initialized with rotary dial sound: $dialSoundId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sounds", e)
        }
    }
    
    private fun initializeMediaPlayer() {
        try {
            val dialResId = context.resources.getIdentifier("rotary_dial_full", "raw", context.packageName)
            Log.d(TAG, "Initializing MediaPlayer with resource ID: $dialResId")
            
            if (dialResId != 0) {
                mediaPlayer = MediaPlayer.create(context, dialResId)
                if (mediaPlayer != null) {
                    Log.d(TAG, "MediaPlayer created successfully")
                    mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mediaPlayer?.setVolume(0.8f, 0.8f)
                } else {
                    Log.e(TAG, "MediaPlayer.create() returned null")
                }
            } else {
                Log.e(TAG, "Cannot create MediaPlayer: resource ID is 0")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaPlayer", e)
        }
    }
    
    /**
     * Play tick sound during drag (very low volume, subtle)
     */
    fun playDragTick(volume: Float = 0.12f) {
        if (!isLoaded || tickSoundId == 0) return
        
        try {
            soundPool?.play(tickSoundId, volume, volume, 0, 0, 1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing drag tick sound", e)
        }
    }
    
    /**
     * Start looped rotary sound while the dial is being dragged.
     */
    fun startDragLoop() {
        Log.d(TAG, "startDragLoop() called - isLoaded: $isLoaded, dialSoundId: $dialSoundId, dragStreamId: $dragStreamId")
        
        if (!isLoaded || dialSoundId == 0) {
            Log.w(TAG, "Dial sound not loaded for drag loop - isLoaded: $isLoaded, dialSoundId: $dialSoundId")
            return
        }
        
        if (dragStreamId != 0) {
            // already playing
            Log.d(TAG, "Drag loop already playing with streamId: $dragStreamId")
            return
        }
        
        try {
            // loop = -1 to loop continuously
            dragStreamId = soundPool?.play(
                dialSoundId,
                1.0f, 1.0f,     // maximum volume
                1,              // priority
                -1,             // loop forever
                1.0f            // rate
            ) ?: 0
            
            Log.d(TAG, "startDragLoop: started with streamId=$dragStreamId")
            
            if (dragStreamId == 0) {
                Log.e(TAG, "SoundPool.play() returned 0 - failed to start")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting drag loop", e)
        }
    }
    
    /**
     * Stop the rotary drag loop sound.
     */
    fun stopDragLoop() {
        if (dragStreamId != 0) {
            soundPool?.stop(dragStreamId)
            Log.d(TAG, "stopDragLoop: stopped stream $dragStreamId")
            dragStreamId = 0
        }
    }
    
    /**
     * Play mechanical tick sound using ToneGenerator (UNUSED - kept for reference)
     */
    fun playTick() {
        // This method is no longer used for drag sounds
        // The drag loop handles continuous sound during rotation
        Log.d(TAG, "playTick() called (unused)")
    }
    
    /**
     * Play tick sound during return animation (normal volume)
     */
    fun playReturnTick(volume: Float = 0.28f) {
        if (!isLoaded || tickSoundId == 0) return
        
        try {
            soundPool?.play(tickSoundId, volume, volume, 0, 0, 1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing return tick sound", e)
        }
    }
    
    /**
     * Play clack sound (when dial is released at stop point)
     */
    fun playClack(volume: Float = 0.5f) {
        if (!isLoaded || clackSoundId == 0) return
        
        try {
            soundPool?.play(clackSoundId, volume, volume, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing clack sound", e)
        }
    }
    
    /**
     * Play the full rotary dial sequence sound
     */
    fun playDialSequence() {
        Log.d(TAG, "playDialSequence() called - isLoaded: $isLoaded, dialSoundId: $dialSoundId, isDialSoundPlaying: $isDialSoundPlaying")
        
        // Don't overlap - ignore if already playing
        if (isDialSoundPlaying) {
            Log.d(TAG, "Dial sound already playing, ignoring")
            return
        }
        
        // Try MediaPlayer first (more reliable for single sounds)
        if (mediaPlayer != null) {
            try {
                isDialSoundPlaying = true
                Log.d(TAG, "Playing rotary dial sound using MediaPlayer")
                
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                    mediaPlayer?.prepare()
                }
                
                mediaPlayer?.start()
                Log.d(TAG, "MediaPlayer started successfully")
                
                // Reset playing flag after sound duration
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    isDialSoundPlaying = false
                    Log.d(TAG, "Reset isDialSoundPlaying flag (MediaPlayer)")
                }, 2000)
                
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error playing sound with MediaPlayer", e)
                isDialSoundPlaying = false
            }
        }
        
        // Fallback to SoundPool
        if (isLoaded && dialSoundId != 0) {
            try {
                isDialSoundPlaying = true
                Log.d(TAG, "Playing rotary dial sequence sound with SoundPool ID: $dialSoundId")
                
                val streamId = soundPool?.play(dialSoundId, 0.8f, 0.8f, 1, 0, 1.0f)
                Log.d(TAG, "SoundPool.play() returned streamId: $streamId")
                
                // Reset playing flag after sound duration (estimate 2 seconds)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    isDialSoundPlaying = false
                    Log.d(TAG, "Reset isDialSoundPlaying flag (SoundPool)")
                }, 2000)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error playing dial sequence sound with SoundPool", e)
                isDialSoundPlaying = false
            }
        } else {
            Log.w(TAG, "Cannot play sound - SoundPool not loaded or sound ID is 0")
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            // Stop any active drag loop
            stopDragLoop()
            
            // Release ToneGenerator
            toneGenerator?.release()
            toneGenerator = null
            
            // Release MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = null
            
            // Release SoundPool
            soundPool?.release()
            soundPool = null
            isLoaded = false
            
            Log.d(TAG, "Sound resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing sound resources", e)
        }
    }
}
