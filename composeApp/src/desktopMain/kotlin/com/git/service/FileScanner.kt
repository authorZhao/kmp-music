package com.git.service

import com.git.model.Music
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 文件扫描服务
 */
class FileScanner {
    
    companion object {
        // 支持的音频格式
        private val SUPPORTED_FORMATS = setOf(
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "wma", "mp4"
        )
    }
    
    /**
     * 扫描目录中的音乐文件
     */
    suspend fun scanDirectory(
        directoryPath: String,
        includeSubdirectories: Boolean = true,
        onProgress: (current: Int, total: Int, fileName: String) -> Unit = { _, _, _ -> }
    ): List<Music> = withContext(Dispatchers.IO) {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext emptyList()
        }
        
        val musicFiles = mutableListOf<File>()
        collectMusicFiles(directory, includeSubdirectories, musicFiles)
        
        val musicList = mutableListOf<Music>()
        musicFiles.forEachIndexed { index, file ->
            onProgress(index + 1, musicFiles.size, file.name)
            try {
                val music = parseAudioFile(file)
                if (music != null) {
                    musicList.add(music)
                }
            } catch (e: Exception) {
                println("解析音频文件失败: ${file.absolutePath}, 错误: ${e.message}")
            }
        }
        
        musicList
    }
    
    /**
     * 递归收集音乐文件
     */
    private fun collectMusicFiles(
        directory: File,
        includeSubdirectories: Boolean,
        musicFiles: MutableList<File>
    ) {
        directory.listFiles()?.forEach { file ->
            when {
                file.isFile && isSupportedAudioFile(file) -> {
                    musicFiles.add(file)
                }
                file.isDirectory && includeSubdirectories -> {
                    collectMusicFiles(file, includeSubdirectories, musicFiles)
                }
            }
        }
    }
    
    /**
     * 检查是否为支持的音频文件
     */
    private fun isSupportedAudioFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in SUPPORTED_FORMATS
    }
    
    /**
     * 解析音频文件元数据
     */
    private fun parseAudioFile(file: File): Music? {
        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            val header = audioFile.audioHeader
            
            // 基本信息
            val title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() } 
                ?: file.nameWithoutExtension
            val artist = tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() } 
                ?: "未知艺术家"
            val album = tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() } 
                ?: "未知专辑"
            
            // 音频信息
            val duration = (header?.trackLength?.toLong() ?: 0L) * 1000L // 转换为毫秒
            val bitrate = header?.bitRateAsNumber?.toInt() ?: 0
            val sampleRate = header?.sampleRateAsNumber?.toInt() ?: 0
            val format = file.extension.lowercase()
            
            // 查找封面图片
            val coverArtPath = findCoverArt(file)
            
            // 查找歌词文件
            val lyricsPath = findLyricsFile(file)
            
            return Music(
                id = UUID.randomUUID().toString(),
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                filePath = file.absolutePath,
                fileSize = file.length(),
                format = format,
                bitrate = bitrate,
                sampleRate = sampleRate,
                coverArtPath = coverArtPath,
                lyricsPath = lyricsPath
            )
            
        } catch (e: CannotReadException) {
            println("无法读取音频文件: ${file.absolutePath}")
            return null
        } catch (e: Exception) {
            println("解析音频文件异常: ${file.absolutePath}, 错误: ${e.message}")
            return null
        }
    }
    
    /**
     * 查找封面图片
     */
    private fun findCoverArt(audioFile: File): String? {
        val directory = audioFile.parentFile
        val baseName = audioFile.nameWithoutExtension
        
        // 常见的封面图片文件名
        val coverNames = listOf(
            "cover", "folder", "album", "front", 
            baseName, // 同名图片
            "albumart", "albumartsmall"
        )
        
        val imageExtensions = listOf("jpg", "jpeg", "png", "bmp", "gif")
        
        for (name in coverNames) {
            for (ext in imageExtensions) {
                val coverFile = File(directory, "$name.$ext")
                if (coverFile.exists()) {
                    return coverFile.absolutePath
                }
            }
        }
        
        return null
    }
    
    /**
     * 查找歌词文件
     */
    private fun findLyricsFile(audioFile: File): String? {
        val directory = audioFile.parentFile
        val baseName = audioFile.nameWithoutExtension
        
        val lyricsExtensions = listOf("lrc", "txt")
        
        for (ext in lyricsExtensions) {
            val lyricsFile = File(directory, "$baseName.$ext")
            if (lyricsFile.exists()) {
                return lyricsFile.absolutePath
            }
        }
        
        return null
    }
    
    /**
     * 验证音频文件是否可播放
     */
    fun validateAudioFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            
            val audioFile = AudioFileIO.read(file)
            audioFile.audioHeader != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取音频文件时长
     */
    fun getAudioDuration(filePath: String): Long {
        return try {
            val file = File(filePath)
            val audioFile = AudioFileIO.read(file)
            (audioFile.audioHeader?.trackLength?.toLong() ?: 0L) * 1000L
        } catch (e: Exception) {
            0L
        }
    }
}
