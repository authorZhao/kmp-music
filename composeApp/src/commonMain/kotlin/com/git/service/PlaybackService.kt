package com.git.service

import com.git.model.Music
import com.git.model.PlayMode
import com.git.model.PlayState
import com.git.model.PlayerState
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放控制服务接口
 */
expect class PlaybackService(
    musicService: MusicService
) {
    // 播放器状态
    val playerState: StateFlow<PlayerState>
    
    // 播放队列
    val playQueue: StateFlow<List<Music>>
    
    // 当前播放索引
    val currentIndex: StateFlow<Int>
    
    /**
     * 设置播放队列
     */
    fun setPlayQueue(musicList: List<Music>, startIndex: Int = 0)
    
    /**
     * 播放指定音乐
     */
    fun playMusic(music: Music)
    
    /**
     * 播放/暂停切换
     */
    fun togglePlayPause()
    
    /**
     * 播放
     */
    fun play()
    
    /**
     * 暂停
     */
    fun pause()
    
    /**
     * 停止
     */
    fun stop()
    
    /**
     * 下一首
     */
    fun next()
    
    /**
     * 上一首
     */
    fun previous()
    
    /**
     * 设置播放模式
     */
    fun setPlayMode(mode: PlayMode)
    
    /**
     * 设置播放位置
     */
    fun seekTo(positionMs: Long)
    
    /**
     * 设置音量
     */
    fun setVolume(volume: Float)
    
    /**
     * 切换静音状态
     */
    fun toggleMute()
    
    /**
     * 获取当前音乐
     */
    fun getCurrentMusic(): Music?
    
    /**
     * 当歌曲播放结束时调用
     */
    fun onSongEnded()
}
