package telegram

import java.io.File

object UserFileManager {
    private const val USERS_DIR = "users"
    private const val WORDS_FILE = "words.txt"
    private const val SETTINGS_FILE = "settings.txt"

    private fun ensureUserDirectory(chatId: Long): File {
        val userDir = File("$USERS_DIR/$chatId")
        if (!userDir.exists()) {
            userDir.mkdirs()

            File(userDir, WORDS_FILE).createNewFile()
            File(userDir, SETTINGS_FILE).createNewFile()
        }
        return userDir
    }

    fun getUserWordsFile(chatId: Long): File {
        ensureUserDirectory(chatId)
        return File("$USERS_DIR/$chatId/$WORDS_FILE")
    }

    fun getUserSettingsFile(chatId: Long): File {
        ensureUserDirectory(chatId)
        return File("$USERS_DIR/$chatId/$SETTINGS_FILE")
    }
}
