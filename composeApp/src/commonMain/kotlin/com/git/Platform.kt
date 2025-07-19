package com.git

import com.git.model.Music

interface Platform {
    companion object{
        val platform: Platform = getPlatform()
    }
    val name: String
    fun uuid():String
    fun currentTimeMillis():Long
    fun format(format: String, vararg args: Any?):String

    // 文件系统相关
    suspend fun selectDirectory(): String?
    suspend fun scanMusicFiles(directoryPath: String, includeSubdirectories: Boolean = true): List<Music>
}

expect fun getPlatform(): Platform
