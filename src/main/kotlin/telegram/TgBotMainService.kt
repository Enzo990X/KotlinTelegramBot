package telegram

import console.WORDS_FILE
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit


class TgBotMainService(private val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()
    private val json: Json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val API_URL = "https://api.telegram.org/bot"
    }

    fun getUpdates(updateId: Long): String {

        return try {
            val urlGetUpdates = "$API_URL$botToken/getUpdates?offset=$updateId&timeout=30"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlGetUpdates))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.body()
        } catch (e: Exception) {
            println("Error getting updates: ${e.message}")
            ""
        }
    }

    fun sendMessage(chatId: Long, message: String) {

        try {
            val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
            val urlSendMessage =
                "$API_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedMessage"

            val requestBody = SendMessageRequest(chatId, message)

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send message: ${e.message}")
        }
    }

    fun sendMenu(chatId: Long) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId, "Меню",
                ReplyMarkup(
                    listOf(
                        listOf(
                            InlineKeyboard(LEARN_WORDS, "Заниматься"),
                            InlineKeyboard(ADD_WORD, "Пополнить словарь")
                        ),
                        listOf(
                            InlineKeyboard(STATS, "Статистика"),
                            InlineKeyboard(SETTINGS, "Настройки")
                        )
                    )
                )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send menu: ${e.message}")
        }
    }

    fun sendGreeting(chatId: Long) {

        try {
            val urlSendPhoto = "$API_URL$botToken/sendPhoto"
            val filePath = "C:/Users/Enzo/IdeaProjects/KotlinTelegramBot/2.1.jpg"
            val imageFile = File(filePath)

            if (!imageFile.exists()) {
                sendMenu(chatId)
                return
            }

            val greetingText = " Привет! Я - твой помощник по изучению иностранных слов, словосочетаний и выражений. " +
                    "Пока у меня есть словарь только английского языка и он содержит слова разного уровня сложности. " +
                    "Можешь загрузить его или начать с пустого, который будешь наполнять самостоятельно."

            val replyMarkup = """
            {
                "inline_keyboard": [
                    [{"text": "Загрузить словарь", "callback_data": "$LOAD_DICTIONARY"}],
                    [{"text": "Начать с пустого словаря", "callback_data": "$EMPTY_DICTIONARY"}]
                ]
            }
        """.trimIndent()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId.toString())
                .addFormDataPart(
                    "photo",
                    "welcome.jpg",
                    imageFile.asRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("caption", greetingText)
                .addFormDataPart("parse_mode", "HTML")
                .addFormDataPart("reply_markup", replyMarkup)
                .build()

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url(urlSendPhoto)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                sendMenu(chatId)
            }
        } catch (e: Exception) {
            sendMenu(chatId)
        }
    }

    fun handleLoadDictionary(chatId: Long) {

        try {
            val defaultWordsFile = File(WORDS_FILE)
            val userWordsFile = UserFileManager.getUserWordsFile(chatId)

            if (!defaultWordsFile.exists()) {
                userWordsFile.writeText("")

                TgBotMainService(botToken).sendMessage(chatId,
                    "Словарь не найден. Создан пустой словарь.")
            } else {
                defaultWordsFile.copyTo(userWordsFile, overwrite = true)
                TgBotMainService(botToken).sendMessage(chatId, "Словарь успешно загружен!")
            }


            TgBotMainService(botToken).sendMenu(chatId)
        } catch (e: Exception) {
            println("Error loading dictionary: ${e.message}")
            TgBotMainService(botToken).sendMessage(chatId, "Произошла ошибка при загрузке словаря")
        }
    }

    fun handleEmptyDictionary(chatId: Long) {

        try {
            val userWordsFile = UserFileManager.getUserWordsFile(chatId)
            userWordsFile.writeText("")

            TgBotMainService(botToken).sendMessage(chatId, "Пустой словарь создан!")

            TgBotMainService(botToken).sendMenu(chatId)
        } catch (e: Exception) {
            println("Error emptying dictionary: ${e.message}")
            TgBotMainService(botToken).sendMessage(chatId, "Произошла ошибка при создании словаря")
        }
    }

    fun sendSettingsMenu(chatId: Long) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                "Настройки",
                ReplyMarkup(
                    listOf(
                        listOf(
                        InlineKeyboard(CHANGE_NUMBER_OF_ITERATIONS, "Размер занятия"),
                        InlineKeyboard(CHANGE_TYPE_OF_TRAIN, "Тип занятия")
                    ),
                    listOf(
                        InlineKeyboard(RESET_PROGRESS, "Сбросить прогресс"),
                        InlineKeyboard(SUPPORT, "Поддержка")
                    ),
                    listOf(InlineKeyboard(MENU, "Вернуться в меню"))
                )
            )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send settings menu: ${e.message}")
        }
    }

    fun sendSupportInfo(chatId: Long) {

        try {
            val supportMessage = """
            Создатель и поддержка: @Enzo990X
            
            По всем вопросам, проблемам и предложениям пишите в личные сообщения.
            Я постараюсь ответить как можно скорее!
            
            Спасибо, что используете моего бота!
        """.trimIndent()

            val urlSendMessage = "$API_URL$botToken/sendMessage"
            val requestBody = SendMessageRequest(
                chatId, supportMessage, ReplyMarkup(listOf(listOf(InlineKeyboard(SETTINGS, "Назад")))))

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Error sending support info: ${e.message}")
        }
    }
}

const val LOAD_DICTIONARY = "load_dictionary"
const val EMPTY_DICTIONARY = "empty_dictionary"

const val TIMEOUT = 60L
