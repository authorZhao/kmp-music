package com.git.service

import com.git.audio.AudioPlayer
import com.git.model.Music
import com.git.model.PlayMode
import com.git.model.PlayState
import com.git.model.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 桌面端播放控制服务实现
 */
actual class PlaybackService actual constructor(
    private val musicService: MusicService
) {
    private val audioPlayer = AudioPlayer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 播放器状态
    private val _playerState = MutableStateFlow(PlayerState())
    actual val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // 播放队列
    private val _playQueue = MutableStateFlow<List<Music>>(emptyList())
    actual val playQueue: StateFlow<List<Music>> = _playQueue.asStateFlow()

    // 当前播放索引
    private val _currentIndex = MutableStateFlow(-1)
    actual val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // 随机播放历史
    private val shuffleHistory = mutableListOf<Int>()
    private var shuffleIndex = -1

    init {
        // 监听AudioPlayer的状态变化并同步到PlayerState
        scope.launch {
            combine(
                audioPlayer.playState,
                audioPlayer.currentPosition,
                audioPlayer.duration,
                audioPlayer.volume
            ) { playState, position, duration, volume ->
                _playerState.value = _playerState.value.copy(
                    playState = playState,
                    currentPosition = position,
                    volume = volume
                )
            }.collect { /* 状态已在上面更新 */ }
        }
    }

    /**
     * 设置播放队列
     */
    actual fun setPlayQueue(musicList: List<Music>, startIndex: Int) {
        _playQueue.value = musicList
        _currentIndex.value = if (musicList.isNotEmpty() && startIndex in musicList.indices) {
            startIndex
        } else {
            -1
        }

        // 重置随机播放历史
        resetShuffleHistory()

        updatePlayerState()
    }

    /**
     * 播放指定音乐
     */
    actual fun playMusic(music: Music) {
        println("PlaybackService: 请求播放音乐: ${music.title}")
        val queue = _playQueue.value
        val index = queue.indexOfFirst { it.id == music.id }

        if (index != -1) {
            println("PlaybackService: 音乐在队列中，索引: $index")
            _currentIndex.value = index
            loadAndPlayCurrentMusic()
        } else {
            println("PlaybackService: 音乐不在队列中，添加到队列")
            // 如果音乐不在队列中，将其添加到队列并播放
            val newQueue = queue.toMutableList()
            newQueue.add(music)
            _playQueue.value = newQueue
            _currentIndex.value = newQueue.size - 1
            resetShuffleHistory()
            loadAndPlayCurrentMusic()
        }
    }

    /**
     * 播放/暂停切换
     */
    actual fun togglePlayPause() {
        val currentState = _playerState.value
        when (currentState.playState) {
            PlayState.PLAYING -> pause()
            PlayState.PAUSED -> resume()
            PlayState.STOPPED -> play()
            PlayState.LOADING -> { /* 等待加载完成 */ }
        }
    }

    /**
     * 播放
     */
    actual fun play() {
        val currentMusic = getCurrentMusic()
        if (currentMusic != null) {
            if (audioPlayer.getCurrentMusic()?.id != currentMusic.id) {
                loadAndPlayCurrentMusic()
            } else {
                audioPlayer.play()
            }
        }
    }

    /**
     * 暂停
     */
    actual fun pause() {
        audioPlayer.pause()
    }

    /**
     * 恢复播放
     */
    private fun resume() {
        audioPlayer.play()
    }

    /**
     * 停止
     */
    actual fun stop() {
        audioPlayer.stop()
    }

    /**
     * 下一首
     */
    actual fun next() {
        val nextIndex = getNextIndex()
        if (nextIndex != -1) {
            _currentIndex.value = nextIndex
            loadAndPlayCurrentMusic()
        }
    }

    /**
     * 上一首
     */
    actual fun previous() {
        val prevIndex = getPreviousIndex()
        if (prevIndex != -1) {
            _currentIndex.value = prevIndex
            loadAndPlayCurrentMusic()
        }
    }

    /**
     * 设置播放模式
     */
    actual fun setPlayMode(mode: PlayMode) {
        _playerState.value = _playerState.value.copy(playMode = mode)
        if (mode == PlayMode.SHUFFLE) {
            resetShuffleHistory()
        }
    }

    /**
     * 设置播放位置
     */
    actual fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }

    /**
     * 设置音量
     */
    actual fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        audioPlayer.setVolume(clampedVolume)
        _playerState.value = _playerState.value.copy(volume = clampedVolume)
    }

    /**
     * 切换静音状态
     */
    actual fun toggleMute() {
        val newMutedState = !_playerState.value.isMuted
        _playerState.value = _playerState.value.copy(isMuted = newMutedState)
        if (newMutedState) {
            audioPlayer.setVolume(0f)
        } else {
            audioPlayer.setVolume(_playerState.value.volume)
        }
    }

    /**
     * 获取当前音乐
     */
    actual fun getCurrentMusic(): Music? {
        val queue = _playQueue.value
        val index = _currentIndex.value
        return if (index in queue.indices) queue[index] else null
    }

    /**
     * 当歌曲播放结束时调用
     */
    actual fun onSongEnded() {
        val playMode = _playerState.value.playMode
        when (playMode) {
            PlayMode.LOOP_ONE -> {
                // 单曲循环，重新播放当前歌曲
                seekTo(0L)
                play()
            }
            else -> {
                // 其他模式，播放下一首
                next()
            }
        }
    }

    /**
     * 获取下一首索引
     */
    private fun getNextIndex(): Int {
        val queue = _playQueue.value
        val currentIndex = _currentIndex.value
        val playMode = _playerState.value.playMode

        if (queue.isEmpty()) return -1

        return when (playMode) {
            PlayMode.SEQUENCE -> {
                if (currentIndex < queue.size - 1) currentIndex + 1 else -1
            }
            PlayMode.LOOP_ALL -> {
                (currentIndex + 1) % queue.size
            }
            PlayMode.LOOP_ONE -> {
                currentIndex
            }
            PlayMode.SHUFFLE -> {
                getNextShuffleIndex()
            }
        }
    }

    /**
     * 获取上一首索引
     */
    private fun getPreviousIndex(): Int {
        val queue = _playQueue.value
        val currentIndex = _currentIndex.value
        val playMode = _playerState.value.playMode

        if (queue.isEmpty()) return -1

        return when (playMode) {
            PlayMode.SEQUENCE -> {
                if (currentIndex > 0) currentIndex - 1 else -1
            }
            PlayMode.LOOP_ALL -> {
                if (currentIndex > 0) currentIndex - 1 else queue.size - 1
            }
            PlayMode.LOOP_ONE -> {
                currentIndex
            }
            PlayMode.SHUFFLE -> {
                getPreviousShuffleIndex()
            }
        }
    }

    /**
     * 获取下一个随机播放索引
     */
    private fun getNextShuffleIndex(): Int {
        val queue = _playQueue.value
        if (queue.isEmpty()) return -1

        // 如果历史记录为空，初始化
        if (shuffleHistory.isEmpty()) {
            shuffleHistory.addAll(queue.indices.shuffled())
            shuffleIndex = shuffleHistory.indexOf(_currentIndex.value)
        }

        // 移动到下一个
        shuffleIndex++

        // 如果到达末尾，重新洗牌
        if (shuffleIndex >= shuffleHistory.size) {
            shuffleHistory.clear()
            shuffleHistory.addAll(queue.indices.shuffled())
            shuffleIndex = 0
        }

        return shuffleHistory[shuffleIndex]
    }

    /**
     * 获取上一个随机播放索引
     */
    private fun getPreviousShuffleIndex(): Int {
        if (shuffleHistory.isEmpty()) return -1

        shuffleIndex--
        if (shuffleIndex < 0) {
            shuffleIndex = 0
        }

        return shuffleHistory[shuffleIndex]
    }

    /**
     * 重置随机播放历史
     */
    private fun resetShuffleHistory() {
        shuffleHistory.clear()
        shuffleIndex = -1
    }

    /**
     * 加载并播放当前音乐
     */
    private fun loadAndPlayCurrentMusic() {
        val currentMusic = getCurrentMusic()
        if (currentMusic != null) {
            println("PlaybackService: 开始加载音乐: ${currentMusic.title}")
            audioPlayer.loadMusic(currentMusic)
            updatePlayerState()
            // 加载完成后自动播放
            scope.launch {
                // 等待加载完成
                kotlinx.coroutines.delay(100) // 短暂延迟确保加载完成
                println("PlaybackService: 开始播放音乐: ${currentMusic.title}")
                audioPlayer.play()
            }
        }
    }

    /**
     * 更新播放器状态
     */
    private fun updatePlayerState() {
        val currentMusic = getCurrentMusic()
        _playerState.value = _playerState.value.copy(
            currentMusic = currentMusic
        )
    }
}
