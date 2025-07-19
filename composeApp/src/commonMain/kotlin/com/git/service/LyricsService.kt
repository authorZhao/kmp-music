package com.git.service

import com.git.model.LyricLine
import com.git.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 歌词服务
 */
class LyricsService {
    
    /**
     * 解析LRC歌词文件
     */
    suspend fun parseLrcFile(filePath: String): Lyrics? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            
            val lines = file.readLines()
            val lyricLines = mutableListOf<LyricLine>()
            var offset = 0L
            
            for (line in lines) {
                when {
                    // 解析时间标签 [mm:ss.xx]
                    line.matches(Regex("\\[\\d{2}:\\d{2}\\.\\d{2}\\].*")) -> {
                        val timeMatch = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)").find(line)
                        if (timeMatch != null) {
                            val (minutes, seconds, centiseconds, text) = timeMatch.destructured
                            val timeMs = minutes.toLong() * 60 * 1000 + 
                                        seconds.toLong() * 1000 + 
                                        centiseconds.toLong() * 10
                            lyricLines.add(LyricLine(timeMs, text.trim()))
                        }
                    }
                    // 解析时间标签 [mm:ss]
                    line.matches(Regex("\\[\\d{2}:\\d{2}\\].*")) -> {
                        val timeMatch = Regex("\\[(\\d{2}):(\\d{2})\\](.*)").find(line)
                        if (timeMatch != null) {
                            val (minutes, seconds, text) = timeMatch.destructured
                            val timeMs = minutes.toLong() * 60 * 1000 + seconds.toLong() * 1000
                            lyricLines.add(LyricLine(timeMs, text.trim()))
                        }
                    }
                    // 解析偏移量 [offset:xxx]
                    line.startsWith("[offset:") -> {
                        val offsetMatch = Regex("\\[offset:(-?\\d+)\\]").find(line)
                        if (offsetMatch != null) {
                            offset = offsetMatch.groupValues[1].toLong()
                        }
                    }
                }
            }
            
            // 按时间排序
            lyricLines.sortBy { it.timeMs }
            
            Lyrics(
                musicId = "", // 需要外部设置
                lines = lyricLines,
                offset = offset
            )
        } catch (e: Exception) {
            println("解析歌词文件失败: $filePath, 错误: ${e.message}")
            null
        }
    }
    
    /**
     * 解析纯文本歌词文件
     */
    suspend fun parseTextFile(filePath: String): Lyrics? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            
            val lines = file.readLines()
            val lyricLines = lines.mapIndexed { index, line ->
                // 为纯文本歌词分配时间，每行间隔3秒
                LyricLine(index * 3000L, line.trim())
            }.filter { it.text.isNotEmpty() }
            
            Lyrics(
                musicId = "", // 需要外部设置
                lines = lyricLines,
                offset = 0L
            )
        } catch (e: Exception) {
            println("解析文本歌词文件失败: $filePath, 错误: ${e.message}")
            null
        }
    }
    
    /**
     * 根据当前播放时间获取当前歌词行
     */
    fun getCurrentLyricLine(lyrics: Lyrics, currentTimeMs: Long): LyricLine? {
        val adjustedTime = currentTimeMs + lyrics.offset
        
        // 找到当前时间对应的歌词行
        var currentLine: LyricLine? = null
        for (line in lyrics.lines) {
            if (line.timeMs <= adjustedTime) {
                currentLine = line
            } else {
                break
            }
        }
        
        return currentLine
    }
    
    /**
     * 获取当前歌词行的索引
     */
    fun getCurrentLyricIndex(lyrics: Lyrics, currentTimeMs: Long): Int {
        val adjustedTime = currentTimeMs + lyrics.offset
        
        for (i in lyrics.lines.indices.reversed()) {
            if (lyrics.lines[i].timeMs <= adjustedTime) {
                return i
            }
        }
        
        return -1
    }
    
    /**
     * 获取下一行歌词
     */
    fun getNextLyricLine(lyrics: Lyrics, currentTimeMs: Long): LyricLine? {
        val currentIndex = getCurrentLyricIndex(lyrics, currentTimeMs)
        return if (currentIndex >= 0 && currentIndex < lyrics.lines.size - 1) {
            lyrics.lines[currentIndex + 1]
        } else {
            null
        }
    }
    
    /**
     * 检查歌词文件是否存在
     */
    fun hasLyrics(lyricsPath: String?): Boolean {
        return lyricsPath != null && File(lyricsPath).exists()
    }
}
