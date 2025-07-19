package com.git.audio

import com.git.model.Music
import com.git.model.PlayState
import javazoom.jl.player.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import javax.sound.sampled.*

/**
 * 桌面端音频播放器实现（使用JLayer和Java Sound API）
 */
class AudioPlayer {
    private var currentMusic: Music? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var player: Player? = null
    private var clip: Clip? = null
    private var audioInputStream: AudioInputStream? = null
    private var isProgressUpdateRunning = false
    private var isPaused = false
    private var pausePosition = 0L
    
    // 播放状态
    private val _playState = MutableStateFlow(PlayState.STOPPED)
    val playState: StateFlow<PlayState> = _playState.asStateFlow()
    
    // 当前位置（毫秒）
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    // 总时长（毫秒）
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    // 音量 (0.0 - 1.0)
    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    init {
        // 初始化音频系统
        println("AudioPlayer 初始化完成")
    }
    
    /**
     * 加载音乐文件
     */
    fun loadMusic(music: Music) {
        try {
            // 停止当前播放
            stop()

            currentMusic = music
            _playState.value = PlayState.LOADING

            val file = File(music.filePath)
            if (!file.exists()) {
                println("音乐文件不存在，使用模拟播放: ${music.filePath}")
                // 对于示例音乐，使用模拟播放
                _duration.value = music.duration
                _playState.value = PlayState.PAUSED
                isPaused = false
                pausePosition = 0L
                println("已加载音乐（模拟）: ${music.title}")
                return
            }

            scope.launch {
                try {
                    // 根据文件类型选择播放方式
                    when (file.extension.lowercase()) {
                        "mp3" -> {
                            // 使用JLayer播放MP3
                            val inputStream = BufferedInputStream(FileInputStream(file))
                            player = Player(inputStream)
                        }
                        "wav", "au", "aiff" -> {
                            // 使用Java Sound API播放WAV等格式
                            audioInputStream = AudioSystem.getAudioInputStream(file)
                            clip = AudioSystem.getClip()
                            clip?.open(audioInputStream)
                        }
                        else -> {
                            println("不支持的音频格式: ${file.extension}")
                            _playState.value = PlayState.STOPPED
                            return@launch
                        }
                    }

                    _duration.value = music.duration
                    _playState.value = PlayState.PAUSED
                    isPaused = false
                    pausePosition = 0L
                    println("已加载音乐: ${music.title}")
                } catch (e: Exception) {
                    println("加载音乐失败: ${e.message}")
                    _playState.value = PlayState.STOPPED
                }
            }

        } catch (e: Exception) {
            println("加载音乐异常: ${e.message}")
            _playState.value = PlayState.STOPPED
        }
    }
    
    /**
     * 播放
     */
    fun play() {
        try {
            val file = currentMusic?.filePath?.let { File(it) }

            if (file?.exists() == true) {
                // 真实文件播放
                if (isPaused) {
                    // 恢复播放
                    clip?.start()
                    isPaused = false
                } else {
                    // 开始新的播放
                    when (currentMusic?.filePath?.substringAfterLast('.')?.lowercase()) {
                        "mp3" -> {
                            scope.launch {
                                try {
                                    player?.play()
                                } catch (e: Exception) {
                                    println("MP3播放失败: ${e.message}")
                                    _playState.value = PlayState.STOPPED
                                }
                            }
                        }
                        "wav", "au", "aiff" -> {
                            clip?.start()
                        }
                    }
                }
            } else {
                // 模拟播放（用于示例音乐）
                println("开始模拟播放: ${currentMusic?.title}")
            }

            _playState.value = PlayState.PLAYING
            startProgressUpdate()
            println("开始播放: ${currentMusic?.title}")
        } catch (e: Exception) {
            println("播放失败: ${e.message}")
            _playState.value = PlayState.STOPPED
        }
    }

    /**
     * 暂停
     */
    fun pause() {
        try {
            clip?.stop()
            isPaused = true
            _playState.value = PlayState.PAUSED
            isProgressUpdateRunning = false
            println("暂停播放: ${currentMusic?.title}")
        } catch (e: Exception) {
            println("暂停失败: ${e.message}")
        }
    }

    /**
     * 停止
     */
    fun stop() {
        try {
            player?.close()
            player = null

            clip?.stop()
            clip?.close()
            clip = null

            audioInputStream?.close()
            audioInputStream = null

            _playState.value = PlayState.STOPPED
            _currentPosition.value = 0L
            isProgressUpdateRunning = false
            isPaused = false
            pausePosition = 0L
            println("停止播放: ${currentMusic?.title}")
        } catch (e: Exception) {
            println("停止失败: ${e.message}")
        }
    }

    /**
     * 跳转到指定位置
     */
    fun seekTo(positionMs: Long) {
        try {
            clip?.let { c ->
                val framePosition = (positionMs * c.format.frameRate / 1000).toLong()
                c.framePosition = framePosition.coerceIn(0L, c.frameLength.toLong()).toInt()
                _currentPosition.value = positionMs.coerceIn(0L, _duration.value)
            }
            println("跳转到位置: ${positionMs}ms")
        } catch (e: Exception) {
            println("跳转失败: ${e.message}")
        }
    }

    /**
     * 设置音量
     */
    fun setVolume(volume: Float) {
        try {
            val clampedVolume = volume.coerceIn(0f, 1f)
            _volume.value = clampedVolume

            clip?.let { c ->
                if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gainControl = c.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val range = gainControl.maximum - gainControl.minimum
                    val gain = gainControl.minimum + range * clampedVolume
                    gainControl.value = gain
                }
            }

            println("设置音量: $clampedVolume")
        } catch (e: Exception) {
            println("设置音量失败: ${e.message}")
        }
    }

    /**
     * 开始进度更新
     */
    private fun startProgressUpdate() {
        if (isProgressUpdateRunning) return

        isProgressUpdateRunning = true
        scope.launch {
            while (isProgressUpdateRunning && _playState.value == PlayState.PLAYING) {
                try {
                    val file = currentMusic?.filePath?.let { File(it) }

                    if (file?.exists() == true && clip != null) {
                        // 真实文件的进度更新
                        clip?.let { c ->
                            if (c.isRunning) {
                                val framePosition = c.framePosition
                                val frameLength = c.frameLength

                                if (frameLength > 0) {
                                    val progressRatio = framePosition.toDouble() / frameLength.toDouble()
                                    _currentPosition.value = (progressRatio * _duration.value).toLong()
                                }

                                // 检查是否播放结束
                                if (framePosition >= frameLength) {
                                    _playState.value = PlayState.STOPPED
                                    _currentPosition.value = 0L
                                    isProgressUpdateRunning = false
                                }
                            }
                        }
                    } else {
                        // 模拟播放的进度更新
                        val currentPos = _currentPosition.value
                        val duration = _duration.value

                        if (currentPos >= duration && duration > 0) {
                            // 播放结束
                            _playState.value = PlayState.STOPPED
                            _currentPosition.value = 0L
                            isProgressUpdateRunning = false
                            println("模拟播放结束: ${currentMusic?.title}")
                        } else {
                            // 每100ms增加100ms进度
                            _currentPosition.value = (currentPos + 100).coerceAtMost(duration)
                        }
                    }

                    delay(100) // 每100ms更新一次
                } catch (e: Exception) {
                    // 忽略进度更新错误
                    delay(100)
                }
            }
        }
    }

    /**
     * 获取当前音乐
     */
    fun getCurrentMusic(): Music? = currentMusic

    /**
     * 释放资源
     */
    fun release() {
        stop()
    }
}
