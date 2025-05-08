package ktb

import java.io.File

fun main() {

    val wordsFile = File("words.txt")
    wordsFile.createNewFile()

    showMenu(wordsFile)

    val dictionary = loadDictionary(wordsFile)
}

fun showMenu(wordsFile: File) {

    while (true) {
        println("Меню:\n1 - Учить слова\n2 - Добавить слово\n3 - Статистика\n0 - Выход")
        var menuInput = readln().toInt()
        val validMenuInputs = listOf(MENU_ONE, MENU_TWO, MENU_THREE, MENU_ZERO)

        while (menuInput !in validMenuInputs) {
            println("Ошибка. Введите число 1, 2, 3 или 0.")
            menuInput = readln().toInt()
        }

        when (menuInput) {
            MENU_ONE -> learnWords()
            MENU_TWO -> addWordToDictionary(wordsFile)
            MENU_THREE -> showStats()
            MENU_ZERO -> return
        }
    }
}

fun learnWords() {
    println("\nТренировка слов.\n")

}

fun loadDictionary(wordsFile: File): MutableList<Word> {
    val dictionary: MutableList<Word> = mutableListOf()
    addWordToDictionaryFromFile(wordsFile, dictionary)
    return dictionary
}

fun showStats() {
    println("\nВаша статистика.\n")
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
