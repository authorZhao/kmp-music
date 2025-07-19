package com.git.model

import com.git.Platform
import kotlinx.serialization.Serializable

/**
 * 音乐文件数据模型
 */
@Serializable
data class Music(
    val id: String,
    val title: String,
    val artist: String = "未知艺术家",
    val album: String = "未知专辑",
    val duration: Long = 0L, // 时长（毫秒）
    val filePath: String,
    val fileSize: Long = 0L,
    val format: String = "", // 文件格式 mp3, wav, flac等
    val bitrate: Int = 0, // 比特率
    val sampleRate: Int = 0, // 采样率
    val coverArtPath: String? = null, // 封面图片路径
    val lyricsPath: String? = null, // 歌词文件路径
    val addTime: Long = Platform.platform.currentTimeMillis(), // 添加时间
    val playCount: Int = 0, // 播放次数
    val lastPlayTime: Long = 0L, // 最后播放时间
    val isFavorite: Boolean = false, // 是否收藏
    val isDeleted: Boolean = false // 是否软删除
)

/**
 * 播放状态枚举
 */
enum class PlayState {
    STOPPED,    // 停止
    PLAYING,    // 播放中
    PAUSED,     // 暂停
    LOADING     // 加载中
}

/**
 * 播放模式枚举
 */
enum class PlayMode {
    SEQUENCE,   // 顺序播放
    LOOP_ALL,   // 列表循环
    LOOP_ONE,   // 单曲循环
    SHUFFLE     // 随机播放
}

/**
 * 播放器状态数据模型
 */
@Serializable
data class PlayerState(
    val currentMusic: Music? = null,
    val playState: PlayState = PlayState.STOPPED,
    val playMode: PlayMode = PlayMode.SEQUENCE,
    val currentPosition: Long = 0L, // 当前播放位置（毫秒）
    val volume: Float = 1.0f, // 音量 0.0-1.0
    val isMuted: Boolean = false // 是否静音
)
