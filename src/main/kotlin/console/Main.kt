package ktb.console

import trainer.model.Dictionary
import ktb.trainer.LearnWordsTrainer
import trainer.model.Question

fun main() {

    val dictionary = Dictionary()
    val trainer = LearnWordsTrainer(dictionary)

    showMenu(trainer)
}

fun showMenu(trainer: LearnWordsTrainer) {

    while (true) {
        println("Меню:\n1 - Учить слова\n2 - Добавить слово\n3 - Статистика\n4 - Настройки\n0 - Выход")
        var menuInput = readInput()
        val validMenuInputs = listOf(MENU_ONE, MENU_TWO, MENU_THREE, MENU_FOUR, MENU_ZERO)

        while (menuInput !in validMenuInputs) {
            println("Ошибка. Введите число 1, 2, 3, 4 или 0.")
            menuInput = readInput()
        }

        when (menuInput) {
            MENU_ONE -> learnWords(trainer)
            MENU_TWO -> Dictionary().addWordToDictionary()
            MENU_THREE -> showStatistics(trainer)
            MENU_FOUR -> trainer.settings.changeSettings()
            MENU_ZERO -> return
        }
    }
}

fun learnWords(trainer: LearnWordsTrainer) {

    val numberOfWordsToTrain = trainer.settings.numberOfIterations

    trainer.resetUsage()

    repeat(numberOfWordsToTrain) {
        val question = trainer.getNextQuestion()
        trainer.question = question

        if (question == null) {
            println("Нет слов для изучения!")
            return
        }

        println(question.asConsoleString())

        var userAnswer = readInput()
        val validUserAnswers = listOf(ANSWER_ONE, ANSWER_TWO, ANSWER_THREE, ANSWER_FOUR, MENU_ZERO)

        while (userAnswer !in validUserAnswers) {
            println("Ошибка. Введите число 1, 2, 3, 4 или 0.")
            userAnswer = readInput()
        }

        if (userAnswer == 0) return

        val userAnswerIndex = userAnswer - INDEX_UPDATE
        val isCorrect = trainer.checkAnswer(userAnswerIndex)

        if (isCorrect) {
            println("Правильно!\n")
        } else {
            println("Неправильно. Правильный ответ: ${question.learningWord.translations}.\n")
        }
    }

    println("Вы закончили тренировку.")
}

fun showStatistics(trainer: LearnWordsTrainer) {

    val statistics = trainer.getStatistics()
    println(
        "\nВаша статистика.\n" +
                "Выучено ${statistics.learnedWords} из ${statistics.wordsInFile} слов. " +
                "Ваш прогресс ${statistics.progressPercentage}%.\n"
    )
}

fun readInput(): Int {

    while (true) {
        val answer = readln()
        if (answer.isEmpty()) {
            println("Ошибка: пустая строка. Пожалуйста, введите число.")
            continue
        }

        try {
            return answer.toInt()
        } catch (e: NumberFormatException) {
            println("Ошибка: введите действительное число.")
        }
    }
}

fun Question.asConsoleString(): String {

    val stringBuilder = StringBuilder()
    stringBuilder.append("Выберите перевод слова ${this.learningWord.original}:\n")

    this.translationsToPick.forEachIndexed { index, word ->
        stringBuilder.append("${index + INDEX_UPDATE} - ${word.translations}\n")
    }
    stringBuilder.append("0 - выход в меню")

    return stringBuilder.toString()
}

const val MENU_ONE = 1
const val MENU_TWO = 2
const val MENU_THREE = 3
const val MENU_FOUR = 4
const val MENU_ZERO = 0

const val ANSWER_ONE = 1
const val ANSWER_TWO = 2
const val ANSWER_THREE = 3
const val ANSWER_FOUR = 4

const val INDEX_UPDATE = 1

const val WORDS_FILE = "words.txt"
const val SETTINGS_FILE = "settings.txt"
