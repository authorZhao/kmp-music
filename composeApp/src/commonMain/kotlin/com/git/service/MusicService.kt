package com.git.service

import com.git.Platform
import com.git.model.Music
import com.git.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 音乐管理服务
 */
class MusicService {
    // 所有音乐列表
    private val _allMusic = MutableStateFlow<List<Music>>(emptyList())
    val allMusic: StateFlow<List<Music>> = _allMusic.asStateFlow()

    // 当前显示的音乐列表（根据选中的歌单过滤）
    private val _currentMusicList = MutableStateFlow<List<Music>>(emptyList())
    val currentMusicList: StateFlow<List<Music>> = _currentMusicList.asStateFlow()

    // 歌单列表
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // 当前选中的歌单
    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    init {
        // 创建默认的"全部音乐"歌单
        val defaultPlaylist = Playlist(
            id = "all_music",
            name = "全部音乐",
            description = "所有已加载的音乐",
            isDefault = true
        )
        _playlists.value = listOf(defaultPlaylist)
        _currentPlaylist.value = defaultPlaylist
    }

    /**
     * 添加音乐到库中
     */
    fun addMusic(music: Music) {
        val currentList = _allMusic.value.toMutableList()
        // 检查是否已存在
        if (currentList.none { it.filePath == music.filePath }) {
            currentList.add(music)
            _allMusic.value = currentList
            updateCurrentMusicList()
        }
    }

    /**
     * 批量添加音乐
     */
    fun addMusicList(musicList: List<Music>) {
        val currentList = _allMusic.value.toMutableList()
        val existingPaths = currentList.map { it.filePath }.toSet()

        val newMusic = musicList.filter { it.filePath !in existingPaths }
        currentList.addAll(newMusic)
        _allMusic.value = currentList
        updateCurrentMusicList()
    }

    /**
     * 软删除音乐
     */
    fun deleteMusic(musicId: String) {
        val currentList = _allMusic.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == musicId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isDeleted = true)
            _allMusic.value = currentList
            updateCurrentMusicList()
        }
    }

    /**
     * 恢复已删除的音乐
     */
    fun restoreMusic(musicId: String) {
        val currentList = _allMusic.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == musicId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isDeleted = false)
            _allMusic.value = currentList
            updateCurrentMusicList()
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(musicId: String) {
        val currentList = _allMusic.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == musicId }
        if (index != -1) {
            val music = currentList[index]
            currentList[index] = music.copy(isFavorite = !music.isFavorite)
            _allMusic.value = currentList
            updateCurrentMusicList()
        }
    }

    /**
     * 创建新歌单
     */
    fun createPlaylist(name: String, description: String = ""): Playlist {
        val playlist = Playlist(
            id = Platform.platform.uuid(),
            name = name,
            description = description
        )
        val currentPlaylists = _playlists.value.toMutableList()
        currentPlaylists.add(playlist)
        _playlists.value = currentPlaylists
        return playlist
    }

    /**
     * 删除歌单
     */
    fun deletePlaylist(playlistId: String) {
        if (playlistId == "all_music") return // 不能删除默认歌单

        val currentPlaylists = _playlists.value.toMutableList()
        currentPlaylists.removeAll { it.id == playlistId }
        _playlists.value = currentPlaylists

        // 如果删除的是当前歌单，切换到默认歌单
        if (_currentPlaylist.value?.id == playlistId) {
            _currentPlaylist.value = _playlists.value.first { it.isDefault }
            updateCurrentMusicList()
        }
    }

    /**
     * 添加音乐到歌单
     */
    fun addMusicToPlaylist(musicId: String, playlistId: String) {
        val currentPlaylists = _playlists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = currentPlaylists[index]
            val musicIds = playlist.musicIds.toMutableList()
            if (musicId !in musicIds) {
                musicIds.add(musicId)
                currentPlaylists[index] = playlist.copy(
                    musicIds = musicIds,
                    updateTime = Platform.platform.currentTimeMillis()
                )
                _playlists.value = currentPlaylists
                updateCurrentMusicList()
            }
        }
    }

    /**
     * 从歌单移除音乐
     */
    fun removeMusicFromPlaylist(musicId: String, playlistId: String) {
        val currentPlaylists = _playlists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = currentPlaylists[index]
            val musicIds = playlist.musicIds.toMutableList()
            musicIds.remove(musicId)
            currentPlaylists[index] = playlist.copy(
                musicIds = musicIds,
                updateTime = Platform.platform.currentTimeMillis()
            )
            _playlists.value = currentPlaylists
            updateCurrentMusicList()
        }
    }

    /**
     * 切换当前歌单
     */
    fun selectPlaylist(playlist: Playlist) {
        _currentPlaylist.value = playlist
        updateCurrentMusicList()
    }

    /**
     * 设置搜索关键词
     */
    fun setSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
        updateCurrentMusicList()
    }

    /**
     * 更新当前音乐列表
     */
    private fun updateCurrentMusicList() {
        val allMusic = _allMusic.value.filter { !it.isDeleted }
        val currentPlaylist = _currentPlaylist.value
        val keyword = _searchKeyword.value

        var filteredMusic = if (currentPlaylist?.isDefault == true) {
            allMusic
        } else {
            currentPlaylist?.musicIds?.mapNotNull { musicId ->
                allMusic.find { it.id == musicId }
            } ?: emptyList()
        }

        // 应用搜索过滤
        if (keyword.isNotBlank()) {
            filteredMusic = filteredMusic.filter { music ->
                music.title.contains(keyword, ignoreCase = true) ||
                music.artist.contains(keyword, ignoreCase = true) ||
                music.album.contains(keyword, ignoreCase = true)
            }
        }

        _currentMusicList.value = filteredMusic
    }

    /**
     * 根据ID获取音乐
     */
    fun getMusicById(id: String): Music? {
        return _allMusic.value.find { it.id == id && !it.isDeleted }
    }

    /**
     * 获取收藏的音乐
     */
    fun getFavoriteMusic(): List<Music> {
        return _allMusic.value.filter { it.isFavorite && !it.isDeleted }
    }
}
