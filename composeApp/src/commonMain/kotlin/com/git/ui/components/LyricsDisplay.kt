package com.git.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.git.model.Lyrics
import com.git.service.LyricsService
import kotlinx.coroutines.launch

/**
 * 歌词显示组件
 */
@Composable
fun LyricsDisplay(
    lyrics: Lyrics?,
    currentTimeMs: Long,
    modifier: Modifier = Modifier
) {
    val lyricsService = remember { LyricsService() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 当前歌词行索引
    val currentIndex = remember(lyrics, currentTimeMs) {
        lyrics?.let { lyricsService.getCurrentLyricIndex(it, currentTimeMs) } ?: -1
    }
    
    // 自动滚动到当前歌词
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            scope.launch {
                listState.animateScrollToItem(
                    index = maxOf(0, currentIndex - 2), // 显示当前行上方2行
                    scrollOffset = 0
                )
            }
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "歌词",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (lyrics == null || lyrics.lines.isEmpty()) {
                // 无歌词时的显示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎵",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "暂无歌词",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // 歌词列表
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(lyrics.lines) { index, lyricLine ->
                        LyricLineItem(
                            lyricLine = lyricLine,
                            isCurrentLine = index == currentIndex,
                            isNextLine = index == currentIndex + 1
                        )
                    }
                    
                    // 底部空白，确保最后一行歌词能滚动到中间
                    item {
                        Spacer(modifier = Modifier.height(200.dp))
                    }
                }
            }
        }
    }
}

/**
 * 歌词行组件
 */
@Composable
private fun LyricLineItem(
    lyricLine: com.git.model.LyricLine,
    isCurrentLine: Boolean,
    isNextLine: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCurrentLine -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        isNextLine -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    }
    
    val textColor = when {
        isCurrentLine -> MaterialTheme.colorScheme.primary
        isNextLine -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }
    
    val fontWeight = when {
        isCurrentLine -> FontWeight.Bold
        isNextLine -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = lyricLine.text.ifEmpty { "♪" },
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 简化版歌词显示（用于播放控制区域）
 */
@Composable
fun SimpleLyricsDisplay(
    lyrics: Lyrics?,
    currentTimeMs: Long,
    modifier: Modifier = Modifier
) {
    val lyricsService = remember { LyricsService() }
    
    val currentLine = remember(lyrics, currentTimeMs) {
        lyrics?.let { lyricsService.getCurrentLyricLine(it, currentTimeMs) }
    }
    
    val nextLine = remember(lyrics, currentTimeMs) {
        lyrics?.let { lyricsService.getNextLyricLine(it, currentTimeMs) }
    }
    
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 当前歌词
        Text(
            text = currentLine?.text ?: "♪",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        
        // 下一行歌词（预览）
        if (nextLine != null && nextLine.text.isNotEmpty()) {
            Text(
                text = nextLine.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
