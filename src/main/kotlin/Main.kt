package ktb

import java.io.File

fun main() {

    val wordsFile = File("words.txt")
    wordsFile.createNewFile()

    val dictionary = loadDictionary(wordsFile)

    showMenu(wordsFile, dictionary)
}

fun showMenu(wordsFile: File, dictionary: List<Word>) {

    while (true) {
        println("Меню:\n1 - Учить слова\n2 - Добавить слово\n3 - Статистика\n0 - Выход\n")
        var menuInput = readln().toInt()
        val validMenuInputs = listOf(MENU_ONE, MENU_TWO, MENU_THREE, MENU_ZERO)

        while (menuInput !in validMenuInputs) {
            println("Ошибка. Введите число 1, 2, 3 или 0.")
            menuInput = readln().toInt()
        }

        when (menuInput) {
            MENU_ONE -> learnWords(dictionary, wordsFile)
            MENU_TWO -> addWordToDictionary(wordsFile)
            MENU_THREE -> showStats(dictionary)
            MENU_ZERO -> return
        }
    }
}

fun learnWords(dictionary: List<Word>, wordsFile: File) {

    val notLearnedList = dictionary.filter { it.correctAnswersCount < NUMBER_OF_CORRECT_ANSWERS }

    if (notLearnedList.isEmpty()) {
        println("Все слова в словаре выучены!")
        return
    }

    val questionWords = notLearnedList.shuffled().take(WORDS_TO_LEARN)

    for (word in questionWords) {
        println("Выберите перевод слова ${word.original}.")

        val incorrectTranslations = dictionary
            .filter { it != word }
            .map { it.translated }
            .shuffled()
            .take(NUMBER_OF_INCORRECT_ANSWERS)
        val translationsToPick = (listOf(word.translated) + incorrectTranslations).shuffled()

        translationsToPick.forEachIndexed { index, translation ->
            println("${index + INDEX_UPDATE} - $translation")
        }

        println("---------")
        println("0 - вернуться в меню")

        var userAnswer = readInput()
        val validUserAnswers = listOf(ANSWER_ONE, ANSWER_TWO, ANSWER_THREE, ANSWER_FOUR, MENU_ZERO)

        while (userAnswer !in validUserAnswers) {
            println("Ошибка. Введите число 1, 2, 3, 4 или 0.")
            userAnswer = readInput()
        }

        if (userAnswer == 0) return

        if (translationsToPick[userAnswer - INDEX_UPDATE] == word.translated) {
            println("Правильно!\n")
            word.correctAnswersCount++
            updateWordInFile(wordsFile, word)
        } else {
            println("Неправильно. Правильный ответ: ${word.translated}.\n")
        }
    }

    println("Вы закончили тренировку.")
}

fun updateWordInFile(wordsFile: File, word: Word) {

    val lines = wordsFile.readLines().toMutableList()

    for (i in lines.indices) {
        val separation = lines[i].split("|").toMutableList()
        if (separation[ORIGINAL_INDEX] == word.original) {
            separation[CORRECT_ANSWERS_COUNT_INDEX] = word.correctAnswersCount.toString()
            lines[i] = separation.joinToString("|")
            break
        }
    }

    wordsFile.writeText(lines.joinToString("\n"))
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

fun loadDictionary(wordsFile: File): List<Word> {
    val dictionary: MutableList<Word> = mutableListOf()
    addWordToDictionaryFromFile(wordsFile, dictionary)
    return dictionary.toList()
}

fun showStats(dictionary: List<Word>) {
    println("\nВаша статистика.")
    val learnedWords = (dictionary.filter { it.correctAnswersCount >= NUMBER_OF_CORRECT_ANSWERS }).size
    val wordsInFile = dictionary.size
    val progressPercentage = (learnedWords.toFloat() / wordsInFile * PERCENTAGE).toInt()

    println("Выучено $learnedWords из $wordsInFile слов. Ваш прогресс $progressPercentage%.\n")
}

fun addWordToDictionary(wordsFile: File) {

    do {
        println("\nВведите новое слово и его перевод: ")
        val newWord = createNewWord()
        wordsFile.appendText(
            "${newWord.original}|${newWord.translated}|${newWord.correctAnswersCount}\n",
            Charsets.UTF_8
        )

        println("\nХотите ввести ещё одно слово?")
    } while (readln().equals("да", ignoreCase = true))
}

fun createNewWord(): Word {
    val input = readWordInput()
    val separation = input.split("|")

    val originalInput = separation[ORIGINAL_INDEX]
    val translatedInput = separation[TRANSLATED_INDEX]

    return Word(originalInput, translatedInput, BASE_CORRECT_ANSWERS_COUNT)
}

fun readWordInput(): String {

    var input = readln().lowercase()
    while (input.isEmpty()) {
        println("Ошибка: пустая строка. Повторите ввод.")
        input = readln().lowercase()
    }

    while (input.split(" ").size != QUANTITY_OF_WORDS) {
        println("Ошибка: введите два слова через пробел.")
        input = readln().lowercase()
    }

    while (!checkWords(input)) {
        println("Ошибка. Первое слово должно быть на английском языке, а второе - на русском. Повторите ввод.")
        input = readln().lowercase()
        checkWords(input)
    }

    input = input.split(" ").joinToString("|")
    return input
}

fun addWordToDictionaryFromFile(wordsFile: File, dictionary: MutableList<Word>) {

    for (line in wordsFile.readLines()) {
        val splitLine = line.split("|")
        val correctAnswersCount = splitLine[CORRECT_ANSWERS_COUNT_INDEX].toShortOrNull() ?: BASE_CORRECT_ANSWERS_COUNT
        val word = Word(splitLine[ORIGINAL_INDEX], splitLine[TRANSLATED_INDEX], correctAnswersCount)
        dictionary.add(word)
    }
}

fun checkWords(input: String): Boolean {
    val inputSplit = input.split(" ")
    return inputSplit[0].all { char ->
        (char in FIRST_EN_SMALL_CHAR..LAST_EN_SMALL_CHAR) ||
                (char in FIRST_EN_BIG_CHAR..LAST_EN_BIG_CHAR)
    } && inputSplit[1].all { char ->
        (char in FIRST_RU_SMALL_CHAR..LAST_RU_SMALL_CHAR) ||
                (char in FIRST_RU_BIG_CHAR..LAST_RU_BIG_CHAR)
    }
}

data class Word(
    val original: String,
    val translated: String,
    var correctAnswersCount: Short = 0,
)

const val QUANTITY_OF_WORDS = 2

private const val FIRST_EN_SMALL_CHAR = 'a'
private const val LAST_EN_SMALL_CHAR = 'z'
private const val FIRST_EN_BIG_CHAR = 'A'
private const val LAST_EN_BIG_CHAR = 'Z'

private const val FIRST_RU_SMALL_CHAR = 'а'
private const val LAST_RU_SMALL_CHAR = 'я'
private const val FIRST_RU_BIG_CHAR = 'А'
private const val LAST_RU_BIG_CHAR = 'Я'

private const val ORIGINAL_INDEX = 0
private const val TRANSLATED_INDEX = 1
private const val CORRECT_ANSWERS_COUNT_INDEX = 2
private const val BASE_CORRECT_ANSWERS_COUNT = 0.toShort()

const val MENU_ONE = 1
const val MENU_TWO = 2
const val MENU_THREE = 3
const val MENU_ZERO = 0

const val ANSWER_ONE = 1
const val ANSWER_TWO = 2
const val ANSWER_THREE = 3
const val ANSWER_FOUR = 4

const val NUMBER_OF_CORRECT_ANSWERS = 3.toShort()
const val WORDS_TO_LEARN = 4
const val NUMBER_OF_INCORRECT_ANSWERS = 3
const val PERCENTAGE = 100

const val INDEX_UPDATE = 1
