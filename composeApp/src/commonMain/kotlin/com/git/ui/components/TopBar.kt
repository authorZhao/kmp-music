package com.git.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 顶部工具栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onTogglePlaylist: () -> Unit,
    onOpenDirectory: () -> Unit,
    onToggleLyrics: () -> Unit = {},
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text("KMP Music Player")
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = onSearchChange,
                    placeholder = { Text("搜索音乐...") },
                    leadingIcon = {
                        Text("🔍")
                    },
                    trailingIcon = {
                        if (searchKeyword.isNotEmpty()) {
                            TextButton(onClick = { onSearchChange("") }) {
                                Text("✕")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.width(300.dp)
                )
                
                // 打开目录按钮
                Button(onClick = onOpenDirectory) {
                    Text("📁 打开目录")
                }

                // 切换歌单侧边栏按钮
                Button(onClick = onTogglePlaylist) {
                    Text("📋 歌单")
                }

                // 歌词显示切换按钮
                Button(onClick = onToggleLyrics) {
                    Text("🎵 歌词")
                }
            }
        },
        modifier = modifier
    )
}
