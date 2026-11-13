package com.mimo.island.source

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

/**
 * 音乐数据源：反向读取系统当前活跃的 MediaSession。
 *
 * 这是唯一真正覆盖「任意音乐 App」的路径 —— 网易云 / QQ音乐 / 酷狗 / Spotify
 * 全都遵守 MediaSession 协议，系统媒体控件就是这么来的。
 *
 * getActiveSessions 必须传一个通知监听组件作为调用方身份，否则系统拒绝返回，
 * 且需要用户授予「通知使用权」。
 */
class MediaSource(private val context: Context) {

    private var controller: MediaController? = null

    private val listenerComponent: ComponentName =
        ComponentName(context, NotifListener::class.java)

    /** 读取当前播放信息。返回 null 表示没有活跃媒体会话或未授权。 */
    fun read(): MusicInfo? {
        val sm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return null
        val sessions = try {
            sm.getActiveSessions(listenerComponent)
        } catch (e: SecurityException) {
            return null
        }
        val session = sessions
            .filter { s -> s.playbackState?.state != PlaybackState.STATE_NONE }
            .maxByOrNull { s -> s.playbackState?.position ?: 0L }
            ?: return null

        val meta: MediaMetadata? = session.metadata
        val state = session.playbackState ?: return null
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: return null

        controller = try {
            MediaController(context, session.sessionToken)
        } catch (e: Exception) {
            null
        }

        return MusicInfo(
            title = title,
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            playing = state.state == PlaybackState.STATE_PLAYING ||
                state.state == PlaybackState.STATE_BUFFERING,
            positionMs = state.position,
            durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            artwork = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
            packageName = session.packageName,
            canControl = controller != null
        )
    }

    /** 把播放/暂停指令转发给当前播放器。 */
    fun togglePlayPause(): Boolean {
        val c = controller ?: return false
        return try {
            if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
                c.transportControls.pause()
            } else {
                c.transportControls.play()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun next(): Boolean = controller?.let {
        try {
            it.transportControls.skipToNext()
            true
        } catch (e: Exception) {
            false
        }
    } ?: false

    fun previous(): Boolean = controller?.let {
        try {
            it.transportControls.skipToPrevious()
            true
        } catch (e: Exception) {
            false
        }
    } ?: false
}
