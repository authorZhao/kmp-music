package com.git.model

import com.git.Platform
import kotlinx.serialization.Serializable

/**
 * 歌单数据模型
 */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val coverPath: String? = null, // 歌单封面
    val musicIds: List<String> = emptyList(), // 音乐ID列表
    val createTime: Long = Platform.platform.currentTimeMillis(),
    val updateTime: Long = Platform.platform.currentTimeMillis(),
    val isDefault: Boolean = false // 是否为默认歌单（全部音乐）
)

/**
 * 歌词行数据模型
 */
@Serializable
data class LyricLine(
    val timeMs: Long, // 时间戳（毫秒）
    val text: String  // 歌词文本
)

/**
 * 歌词数据模型
 */
@Serializable
data class Lyrics(
    val musicId: String,
    val lines: List<LyricLine> = emptyList(),
    val offset: Long = 0L // 时间偏移（毫秒）
)

/**
 * 应用配置数据模型
 */
@Serializable
data class AppConfig(
    val lastMusicDirectory: String = "",
    val defaultPlayMode: PlayMode = PlayMode.SEQUENCE,
    val defaultVolume: Float = 1.0f,
    val autoScanSubdirectories: Boolean = true,
    val supportedFormats: List<String> = listOf("mp3", "wav", "flac", "m4a", "ogg", "wma"),
    val windowWidth: Int = 1200,
    val windowHeight: Int = 800,
    val theme: String = "system" // system, light, dark
)
