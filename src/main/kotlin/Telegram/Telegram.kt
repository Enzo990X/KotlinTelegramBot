package Telegram

import ktb.trainer.LearnWordsTrainer
import trainer.model.Dictionary
import trainer.model.Question

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: java Telegram <bot_token>")
        return
    }

    val updateIdRegex = Regex("update_id\":(\\d+)")
    val messageTextRegex = Regex("\"text\":\"([^\"]+)\"")
    val chatIdRegex = Regex("chat\":\\{\"id\":(\\d+)")
    val dataRegex = Regex("\"data\":\"([^\"]+)\"")
    val callbackQueryIdRegex = Regex("callback_query\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"")


    val botToken = args[FIRST_INDEX]
    var updateId = START_UPDATE_ID

    val service = TelegramBotService(botToken)
    val trainingSessions = mutableMapOf<String, TrainingSession>()

    while (true) {
        Thread.sleep(SLEEP)
        val updates = service.getUpdates(botToken, updateId)

        val updateIdMatch = updateIdRegex.find(updates)
        val updateIdString = updateIdMatch?.groupValues?.get(SECOND_INDEX)
        val messageText = messageTextRegex.find(updates)?.groupValues?.get(SECOND_INDEX)
        val chatIdMatch = chatIdRegex.find(updates)
        val chatId = chatIdMatch?.groupValues?.get(SECOND_INDEX) ?: continue
        val data = dataRegex.find(updates)?.groupValues?.get(SECOND_INDEX)
        val callbackQueryId = callbackQueryIdRegex.find(updates)?.groupValues?.get(SECOND_INDEX)


        fun askNextQuestion(service: TelegramBotService, botToken: String, chatId: String, session: TrainingSession) {
            try {
                println("Checking if session is complete...")
                if (session.isComplete) {
                    println("Training session is complete")
                    service.sendMessage(botToken, chatId, "🏁 Тренировка завершена!")
                    trainingSessions.remove(chatId)
                    return
                }

                println("Getting next question...")
                val question = session.getNextQuestion()
                if (question == null) {
                    println("No more questions available")
                    service.sendMessage(botToken, chatId, "Нет слов для изучения!")
                    trainingSessions.remove(chatId)
                    return
                }

                println("Sending question for word: ${question.learningWord.original}")
                service.sendQuestion(botToken, chatId, question)
            } catch (e: Exception) {
                println("Error in askNextQuestion: ${e.message}")
                e.printStackTrace()
                service.sendMessage(botToken, chatId, "Произошла ошибка при получении следующего вопроса.")
            }
        }

        fun handleAnswer(selectedIndex: Int) {
            val session = trainingSessions[chatId] ?: return
            val isCorrect = session.recordAnswer(selectedIndex)
            val question = session.currentQuestion ?: return

            val message = if (isCorrect) {
                "✅ Правильно!"
            } else {
                "❌ Неправильно. Правильный ответ: ${question.learningWord.translations}."
            }

            service.sendMessage(botToken, chatId, message)

            // Ask next question or finish training
            askNextQuestion(service, botToken, chatId, session)
        }

        when {
            data != null -> {
                println("Received callback data: $data")  // Debug log
                when {
                    data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                        println("Processing answer: $data")  // Debug log
                        val answerIndex = data.removePrefix(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
                        answerIndex?.let { handleAnswer(it) }
                    }
                    data == LEARN_WORDS -> {
                        println("Starting new training session")  // Debug log
                        try {
                            val trainer = LearnWordsTrainer(Dictionary())
                            trainer.resetUsage()
                            val session = TrainingSession(trainer)
                            trainingSessions[chatId] = session
                            askNextQuestion(service, botToken, chatId, session)
                        } catch (e: Exception) {
                            println("Error in LEARN_WORDS: ${e.message}")
                            e.printStackTrace()
                            service.sendMessage(botToken, chatId, "Произошла ошибка при запуске обучения")
                        }
                    }
                    data == ADD_WORD -> service.sendMessage(botToken, chatId, "Add word")
                    data == STATS -> service.showStats(LearnWordsTrainer(Dictionary()), chatId)
                    data == SETTINGS -> service.sendMessage(botToken, chatId, "Settings")
                    else -> println("Unknown callback data: $data")  // Debug log
                }
            }
            messageText != null -> when (messageText.lowercase()) {
                "учить слова" -> {
                    println("Starting new training session via text command")  // Debug log
                    try {
                        val trainer = LearnWordsTrainer(Dictionary())
                        trainer.resetUsage()
                        val session = TrainingSession(trainer)
                        trainingSessions[chatId] = session
                        askNextQuestion(service, botToken, chatId, session)
                    } catch (e: Exception) {
                        println("Error in text command: ${e.message}")
                        e.printStackTrace()
                        service.sendMessage(botToken, chatId, "Произошла ошибка при запуске обучения")
                    }
                }
                "добавить слово" -> service.sendMessage(botToken, chatId, "Add word")
                "статистика" -> service.showStats(LearnWordsTrainer(Dictionary()), chatId)
                "настройки" -> service.sendMessage(botToken, chatId, "Settings")
                HELLO.lowercase() -> service.sendMessage(botToken, chatId, "Hello!")
                START.lowercase() -> service.sendMenu(botToken, chatId)
                else -> println("Unknown command: $messageText")  // Debug log
            }
        }


        if (updateIdString != null) {
            updateId = updateIdString.toInt() + INCREMENT
        }
    }
}

const val FIRST_INDEX = 0
const val SECOND_INDEX = 1
const val START_UPDATE_ID = 0
const val SLEEP = 2000L
const val INCREMENT = 1

const val LEARN_WORDS = "learn_words"
const val ADD_WORD = "add_word"
const val STATS = "stats"
const val SETTINGS = "settings"

const val HELLO = "Hello"
const val START = "/start"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
