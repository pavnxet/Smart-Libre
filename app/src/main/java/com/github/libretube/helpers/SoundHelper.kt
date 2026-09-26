package com.github.libretube.helpers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.github.libretube.R

object SoundHelper {
    private const val TAG = "SoundHelper"

    /**
     * Plays the Anime Wow sound effect safely across different Android versions
     * using explicit USAGE_ASSISTANCE_SONIFICATION / USAGE_MEDIA audio attributes.
     */
    fun playAnimeWow(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val mp = MediaPlayer()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }

            val afd = context.resources.openRawResourceFd(R.raw.anime_wow)
            if (afd != null) {
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mp.prepare()
                mp.setOnCompletionListener { player ->
                    try {
                        player.reset()
                        player.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing MediaPlayer on complete", e)
                    }
                }
                mp.setOnErrorListener { player, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    try {
                        player.reset()
                        player.release()
                    } catch (e: Exception) {
                        // ignore
                    }
                    true
                }
                mp.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play anime_wow sound", e)
        }
    }
}
