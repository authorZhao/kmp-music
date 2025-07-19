package com.git.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.git.Platform
import com.git.service.LyricsService
import com.git.service.MusicService
import com.git.service.PlaybackService
import com.git.ui.components.LyricsDisplay
import com.git.ui.components.TopBar
import kotlinx.coroutines.launch

/**
 * 音乐播放器主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerApp() {
    // 创建服务实例
    val musicService = remember { MusicService() }
    val playbackService = remember { PlaybackService(musicService) }
    val lyricsService = remember { LyricsService() }
    val platform = remember { Platform.platform }
    
    // 状态
    val currentMusicList by musicService.currentMusicList.collectAsState()
    val playlists by musicService.playlists.collectAsState()
    val currentPlaylist by musicService.currentPlaylist.collectAsState()
    val playerState by playbackService.playerState.collectAsState()
    
    // UI状态
    var showPlaylistSidebar by remember { mutableStateOf(true) }
    var showLyrics by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // 歌词状态
    var currentLyrics by remember { mutableStateOf<com.git.model.Lyrics?>(null) }

    // 加载当前音乐的歌词
    LaunchedEffect(playerState.currentMusic) {
        currentLyrics = null
        val currentMusicSnapshot = playerState.currentMusic
        currentMusicSnapshot?.lyricsPath?.let { lyricsPath ->
            currentLyrics = when {
                lyricsPath.endsWith(".lrc", ignoreCase = true) ->
                    lyricsService.parseLrcFile(lyricsPath)
                lyricsPath.endsWith(".txt", ignoreCase = true) ->
                    lyricsService.parseTextFile(lyricsPath)
                else -> null
            }?.copy(musicId = currentMusicSnapshot.id)
        }
    }
    
    // 协程作用域
    val scope = rememberCoroutineScope()

    // 添加示例音乐数据
    LaunchedEffect(Unit) {
        val sampleMusic = listOf(
            com.git.model.Music(
                id = "1",
                title = "示例歌曲 1",
                artist = "示例艺术家 1",
                album = "示例专辑 1",
                duration = 180000L, // 3分钟
                filePath = "/path/to/music1.mp3"
            ),
            com.git.model.Music(
                id = "2",
                title = "示例歌曲 2",
                artist = "示例艺术家 2",
                album = "示例专辑 2",
                duration = 240000L, // 4分钟
                filePath = "/path/to/music2.mp3"
            ),
            com.git.model.Music(
                id = "3",
                title = "示例歌曲 3",
                artist = "示例艺术家 3",
                album = "示例专辑 3",
                duration = 200000L, // 3分20秒
                filePath = "/path/to/music3.mp3"
            )
        )
        musicService.addMusicList(sampleMusic)
    }
    
    Scaffold(
        topBar = {
            TopBar(
                onTogglePlaylist = { showPlaylistSidebar = !showPlaylistSidebar },
                onToggleLyrics = { showLyrics = !showLyrics },
                onOpenDirectory = {
                    scope.launch {
                        isLoading = true
                        try {
                            val directoryPath = platform.selectDirectory()
                            if (directoryPath != null) {
                                val musicFiles = platform.scanMusicFiles(directoryPath, true)
                                musicService.addMusicList(musicFiles)
                            }
                        } catch (e: Exception) {
                            println("加载音乐失败: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                searchKeyword = musicService.searchKeyword.collectAsState().value,
                onSearchChange = { musicService.setSearchKeyword(it) }
            )
        },
        bottomBar = {
            SimplePlaybackControls(
                playerState = playerState,
                onPlayPause = {
                    println("MusicPlayerApp: 点击播放/暂停按钮，当前状态: ${playerState.playState}")
                    playbackService.togglePlayPause()
                },
                onNext = { playbackService.next() },
                onPrevious = { playbackService.previous() },
                onSeek = { playbackService.seekTo(it) },
                onVolumeChange = { playbackService.setVolume(it) },
                onModeChange = { playbackService.setPlayMode(it) }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 歌单侧边栏
            if (showPlaylistSidebar) {
                SimplePlaylistSidebar(
                    playlists = playlists,
                    currentPlaylist = currentPlaylist,
                    onPlaylistSelect = { musicService.selectPlaylist(it) },
                    onCreatePlaylist = { name, description ->
                        musicService.createPlaylist(name, description)
                    },
                    onDeletePlaylist = { musicService.deletePlaylist(it.id) },
                    modifier = Modifier.width(250.dp)
                )
                
                VerticalDivider()
            }
            
            // 主内容区域
            Row(modifier = Modifier.fillMaxSize()) {
                // 音乐列表
                SimpleMusicList(
                    musicList = currentMusicList,
                    currentMusic = playerState.currentMusic,
                    onMusicSelect = { music ->
                        println("MusicPlayerApp: 点击播放音乐: ${music.title}")
                        playbackService.setPlayQueue(currentMusicList)
                        playbackService.playMusic(music)
                        println("MusicPlayerApp: 播放命令已发送")
                    },
                    onToggleFavorite = { musicService.toggleFavorite(it.id) },
                    onDeleteMusic = { musicService.deleteMusic(it.id) },
                    onAddToPlaylist = { music, playlist ->
                        musicService.addMusicToPlaylist(music.id, playlist.id)
                    },
                    playlists = playlists,
                    isLoading = isLoading,
                    modifier = Modifier.weight(if (showLyrics) 0.6f else 1f)
                )

                // 歌词显示
                if (showLyrics) {
                    VerticalDivider()
                    LyricsDisplay(
                        lyrics = currentLyrics,
                        currentTimeMs = playerState.currentPosition,
                        modifier = Modifier.weight(0.4f)
                    )
                }
            }
        }
    }
}
