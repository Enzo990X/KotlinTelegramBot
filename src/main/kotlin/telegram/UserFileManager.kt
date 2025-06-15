package telegram

import java.io.File

object UserFileManager {
    private const val BASE_DIR = "/opt/telegram-bot"
    private const val USERS_DIR = "$BASE_DIR/users"
    private const val WORDS_FILE = "words.txt"
    private const val SETTINGS_FILE = "settings.txt"

    val GLOBAL_WORDS_FILE = File("$BASE_DIR/$WORDS_FILE")
    val GLOBAL_SETTINGS_FILE = File("$BASE_DIR/$SETTINGS_FILE")

    private fun ensureUserDirectory(chatId: Long): File {
        val userDir = File("$USERS_DIR/$chatId")
        if (!userDir.exists()) {
            userDir.mkdirs()
            File(userDir, WORDS_FILE).createNewFile()
            File(userDir, SETTINGS_FILE).createNewFile()

            userDir.setWritable(true, false)
            userDir.setReadable(true, false)
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
