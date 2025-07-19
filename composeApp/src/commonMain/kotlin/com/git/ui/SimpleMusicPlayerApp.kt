package com.git.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.git.Platform
import com.git.model.Music
import com.git.model.PlayState
import com.git.service.MusicService
import com.git.service.PlaybackService

/**
 * 简化版音乐播放器界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMusicPlayerApp() {
    // 创建服务实例
    val musicService = remember { MusicService() }
    val playbackService = remember { PlaybackService(musicService) }

    // 状态
    val currentMusicList by musicService.currentMusicList.collectAsState()
    val playerState by playbackService.playerState.collectAsState()

    // 添加一些示例音乐
    LaunchedEffect(Unit) {
        val sampleMusic = listOf(
            Music(
                id = "1",
                title = "示例歌曲 1",
                artist = "示例艺术家 1",
                album = "示例专辑 1",
                duration = 180000L, // 3分钟
                filePath = "/path/to/music1.mp3"
            ),
            Music(
                id = "2",
                title = "示例歌曲 2",
                artist = "示例艺术家 2",
                album = "示例专辑 2",
                duration = 240000L, // 4分钟
                filePath = "/path/to/music2.mp3"
            ),
            Music(
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
            TopAppBar(
                title = { Text("KMP Music Player") },
                actions = {
                    Button(onClick = { /* TODO: 打开目录 */ }) {
                        Text("打开目录")
                    }
                }
            )
        },
        bottomBar = {
            SimplePlaybackControls(
                playerState = playerState,
                onPlayPause = {
                    println("点击播放/暂停按钮，当前状态: ${playerState.playState}")
                    playbackService.togglePlayPause()
                },
                onNext = { playbackService.next() },
                onPrevious = { playbackService.previous() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "音乐列表 (${currentMusicList.size} 首)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentMusicList) { music ->
                    SimpleMusicItem(
                        music = music,
                        isPlaying = music.id == playerState.currentMusic?.id,
                        onClick = {
                            println("点击播放音乐: ${music.title}")
                            playbackService.setPlayQueue(currentMusicList)
                            playbackService.playMusic(music)
                            println("播放命令已发送")
                        }
                    )
                }
            }
        }
    }
}

/**
 * 简化版音乐项
 */
@Composable
private fun SimpleMusicItem(
    music: Music,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = music.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${music.artist} • ${music.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (isPlaying) {
                    Text(
                        text = "♪",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = formatDuration(music.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 简化版播放控制
 */
@Composable
private fun SimplePlaybackControls(
    playerState: com.git.model.PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 当前音乐信息
            if (playerState.currentMusic != null) {
                Text(
                    text = playerState.currentMusic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = playerState.currentMusic.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onPrevious) {
                    Text("⏮")
                }

                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp)
                ) {
                    Text(
                        when (playerState.playState) {
                            PlayState.PLAYING -> "⏸"
                            PlayState.LOADING -> "⏳"
                            else -> "▶"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Button(onClick = onNext) {
                    Text("⏭")
                }
            }

            // 播放状态
            Text(
                text = "状态: ${
                    when (playerState.playState) {
                        PlayState.PLAYING -> "播放中"
                        PlayState.PAUSED -> "暂停"
                        PlayState.STOPPED -> "停止"
                        PlayState.LOADING -> "加载中"
                    }
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * 格式化时长
 */
private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))

    return if (hours > 0) {
        Platform.platform.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        Platform.platform.format("%d:%02d", minutes, seconds)
    }
}

/**
 * 简化版音乐列表
 */
@Composable
fun SimpleMusicList(
    musicList: List<Music>,
    currentMusic: Music?,
    onMusicSelect: (Music) -> Unit,
    onToggleFavorite: (Music) -> Unit,
    onDeleteMusic: (Music) -> Unit,
    onAddToPlaylist: (Music, com.git.model.Playlist) -> Unit,
    playlists: List<com.git.model.Playlist>,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "音乐列表 (${musicList.size} 首)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("正在扫描音乐文件...")
                }
            }
        } else if (musicList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🎵",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "暂无音乐",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "点击上方的📁 打开目录按钮选择音乐目录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(musicList) { music ->
                    SimpleMusicListItem(
                        music = music,
                        isPlaying = music.id == currentMusic?.id,
                        onClick = { onMusicSelect(music) },
                        onToggleFavorite = { onToggleFavorite(music) },
                        onDelete = { onDeleteMusic(music) },
                        onAddToPlaylist = { playlist -> onAddToPlaylist(music, playlist) },
                        playlists = playlists
                    )
                }
            }
        }
    }
}

/**
 * 简化版音乐列表项
 */
@Composable
private fun SimpleMusicListItem(
    music: Music,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onAddToPlaylist: (com.git.model.Playlist) -> Unit,
    playlists: List<com.git.model.Playlist>,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放状态指示器
            if (isPlaying) {
                Text(
                    text = "♪",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 音乐信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = music.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                )

                Text(
                    text = "${music.artist} • ${music.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = formatDuration(music.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // 收藏按钮
            TextButton(onClick = onToggleFavorite) {
                Text(if (music.isFavorite) "❤️" else "🤍")
            }

            // 更多操作按钮
            TextButton(onClick = { showMenu = true }) {
                Text("⋮")
            }
        }
    }

    // 更多操作菜单
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("操作") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showMenu = false
                            showPlaylistDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("添加到歌单")
                    }

                    TextButton(
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("删除")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 添加到歌单对话框
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("选择歌单") },
            text = {
                LazyColumn {
                    items(playlists.filter { !it.isDefault }) { playlist ->
                        TextButton(
                            onClick = {
                                onAddToPlaylist(playlist)
                                showPlaylistDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(playlist.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除音乐") },
            text = { Text("确定要删除 \"${music.title}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 简化版歌单侧边栏
 */
@Composable
fun SimplePlaylistSidebar(
    playlists: List<com.git.model.Playlist>,
    currentPlaylist: com.git.model.Playlist?,
    onPlaylistSelect: (com.git.model.Playlist) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (com.git.model.Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        // 标题和创建按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "歌单",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(onClick = { showCreateDialog = true }) {
                Text("➕")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 歌单列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(playlists) { playlist ->
                SimplePlaylistItem(
                    playlist = playlist,
                    isSelected = playlist.id == currentPlaylist?.id,
                    onClick = { onPlaylistSelect(playlist) },
                    onDelete = if (!playlist.isDefault) {
                        { onDeletePlaylist(playlist) }
                    } else null
                )
            }
        }
    }

    // 创建歌单对话框
    if (showCreateDialog) {
        SimpleCreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description ->
                onCreatePlaylist(name, description)
                showCreateDialog = false
            }
        )
    }
}

/**
 * 简化版歌单项
 */
@Composable
private fun SimplePlaylistItem(
    playlist: com.git.model.Playlist,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )

                if (playlist.description.isNotEmpty()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // 删除按钮（仅非默认歌单显示）
            if (onDelete != null) {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("🗑")
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除歌单") },
            text = { Text("确定要删除歌单 \"${playlist.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 简化版创建歌单对话框
 */
@Composable
private fun SimpleCreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建歌单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("歌单名称") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 增强版播放控制组件
 */
@Composable
fun SimplePlaybackControls(
    playerState: com.git.model.PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onModeChange: (com.git.model.PlayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 进度条
            if (playerState.currentMusic != null) {
                SimpleProgressBar(
                    currentPosition = playerState.currentPosition,
                    duration = playerState.currentMusic.duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 当前音乐信息
            if (playerState.currentMusic != null) {
                Text(
                    text = playerState.currentMusic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = playerState.currentMusic.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 控制按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 播放模式按钮
                Button(onClick = { onModeChange(getNextPlayMode(playerState.playMode)) }) {
                    Text(
                        when (playerState.playMode) {
                            com.git.model.PlayMode.SEQUENCE -> "➡️"
                            com.git.model.PlayMode.LOOP_ALL -> "🔁"
                            com.git.model.PlayMode.LOOP_ONE -> "🔂"
                            com.git.model.PlayMode.SHUFFLE -> "🔀"
                        }
                    )
                }

                Button(onClick = onPrevious) {
                    Text("⏮")
                }

                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp)
                ) {
                    Text(
                        when (playerState.playState) {
                            com.git.model.PlayState.PLAYING -> "⏸"
                            com.git.model.PlayState.LOADING -> "⏳"
                            else -> "▶"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Button(onClick = onNext) {
                    Text("⏭")
                }

                // 音量控制
                SimpleVolumeControl(
                    volume = playerState.volume,
                    isMuted = playerState.isMuted,
                    onVolumeChange = onVolumeChange
                )
            }

            // 播放状态和模式信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "状态: ${
                        when (playerState.playState) {
                            com.git.model.PlayState.PLAYING -> "播放中"
                            com.git.model.PlayState.PAUSED -> "暂停"
                            com.git.model.PlayState.STOPPED -> "停止"
                            com.git.model.PlayState.LOADING -> "加载中"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = "模式: ${
                        when (playerState.playMode) {
                            com.git.model.PlayMode.SEQUENCE -> "顺序播放"
                            com.git.model.PlayMode.LOOP_ALL -> "列表循环"
                            com.git.model.PlayMode.LOOP_ONE -> "单曲循环"
                            com.git.model.PlayMode.SHUFFLE -> "随机播放"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 简化版进度条
 */
@Composable
private fun SimpleProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    val progress = if (duration > 0) {
        if (isDragging) dragPosition else currentPosition.toFloat() / duration.toFloat()
    } else 0f

    Column(modifier = modifier) {
        Slider(
            value = progress,
            onValueChange = { value ->
                isDragging = true
                dragPosition = value
            },
            onValueChangeFinished = {
                val newPosition = (dragPosition * duration).toLong()
                onSeek(newPosition)
                isDragging = false
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 简化版音量控制
 */
@Composable
private fun SimpleVolumeControl(
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isMuted) "🔇" else if (volume > 0.5f) "🔊" else "🔉",
            style = MaterialTheme.typography.bodyLarge
        )

        Slider(
            value = if (isMuted) 0f else volume,
            onValueChange = onVolumeChange,
            modifier = Modifier.width(100.dp)
        )
    }
}

/**
 * 获取下一个播放模式
 */
private fun getNextPlayMode(currentMode: com.git.model.PlayMode): com.git.model.PlayMode {
    return when (currentMode) {
        com.git.model.PlayMode.SEQUENCE -> com.git.model.PlayMode.LOOP_ALL
        com.git.model.PlayMode.LOOP_ALL -> com.git.model.PlayMode.LOOP_ONE
        com.git.model.PlayMode.LOOP_ONE -> com.git.model.PlayMode.SHUFFLE
        com.git.model.PlayMode.SHUFFLE -> com.git.model.PlayMode.SEQUENCE
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timeMs: Long): String {
    val seconds = (timeMs / 1000) % 60
    val minutes = (timeMs / (1000 * 60)) % 60
    val hours = (timeMs / (1000 * 60 * 60))

    return if (hours > 0) {
        Platform.platform.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        Platform.platform.format("%d:%02d", minutes, seconds)
    }
}
