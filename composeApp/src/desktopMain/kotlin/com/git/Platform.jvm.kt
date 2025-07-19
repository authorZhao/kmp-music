package com.git

import com.git.model.Music
import com.git.service.FileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override fun uuid(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    override fun format(format: String, vararg args: Any?): String {
        return String.format(format, *args)
    }

    override suspend fun selectDirectory(): String? = withContext(Dispatchers.IO) {
        try {
            val fileChooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory)
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            fileChooser.dialogTitle = "选择音乐目录"

            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            println("选择目录失败: ${e.message}")
            null
        }
    }

    override suspend fun scanMusicFiles(directoryPath: String, includeSubdirectories: Boolean): List<Music> {
        val scanner = FileScanner()
        return scanner.scanDirectory(directoryPath, includeSubdirectories) { current, total, fileName ->
            println("扫描进度: $current/$total - $fileName")
        }
    }
}

actual fun getPlatform(): Platform = JVMPlatform()
