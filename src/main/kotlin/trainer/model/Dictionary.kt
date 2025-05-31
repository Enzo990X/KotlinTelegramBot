package trainer.model

import console.WORDS_FILE
import trainer.COLLOCATION_SIZE
import trainer.EXPRESSION_SIZE
import trainer.WORD_SIZE
import java.io.File
import java.lang.IllegalStateException

class Dictionary {

    fun addWordToDictionary() {
        val wordsFile = File(WORDS_FILE)

        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
        }

        do {
            val newWord = createNewWord()
            wordsFile.appendText(
                "${newWord.original}|${newWord.translation}|${newWord.type}|" +
                        "${newWord.correctAnswersCount}|${newWord.usageCount}\n",
                Charsets.UTF_8
            )

            println("\nХотите ввести ещё одно слово?")
        } while (readln().equals("да", ignoreCase = true))
    }

    private fun createNewWord(): Word {

        val input = readWordInput()
        val separation = input.split("|")

        val originalInput = separation[FIRST_INDEX]
        val translatedInput = separation[SECOND_INDEX]
        val typeInput = separation[THIRD_INDEX]

        return Word(originalInput, translatedInput, typeInput, START_CORRECT_ANSWERS_COUNT, START_USAGE_COUNT)
    }

    private fun readWordInput(): String {

        println("\nВведите тип слова: ")
        var typeInput = readln().lowercase()

        val validTypes = listOf("слово", "словосочетание", "выражение")
        while (typeInput !in validTypes) {
            println("Ошибка: введите \"слово\", \"словосочетание\" или \"выражение\".")
            typeInput = readln().lowercase()
        }

        println("\nВведите новое $typeInput на иностранном языке: ")
        var latinInput: String

        while (true) {
            latinInput = readln().lowercase()

            if (latinInput.isEmpty()) {
                println("Ошибка: пустая строка. Повторите ввод.")
                continue
            }

            if (!checkLatinWords(latinInput)) {
                println("Ошибка: используйте только латинские символы. Повторите ввод.")
                continue
            }

            if (!inputCheckForType(typeInput, latinInput)) {
                println("Ошибка: неверное количество слов для типа \"$typeInput\". Повторите ввод.")
                continue
            }

            if (isWordInDictionary(latinInput)) {
                println("Ошибка: \"$latinInput\" уже существует в словаре. Повторите ввод.")
                continue
            }

            break
        }

        println("\nВведите перевод: ")
        var cyrillicInput: String

        while (true) {
            cyrillicInput = readln().lowercase()

            if (cyrillicInput.isEmpty()) {
                println("Ошибка: пустая строка. Повторите ввод.")
                continue
            }

            if (!checkCyrillicWords(cyrillicInput)) {
                println("Ошибка: используйте только кириллические символы. Повторите ввод.")
                continue
            }

            break
        }

        return "$latinInput|$cyrillicInput|$typeInput"
    }


    private fun inputCheckForType(typeInput: String, latinInput: String): Boolean {

        val wordCount = latinInput.split(" ").size

        return when (typeInput) {
            "слово" -> wordCount == WORD_SIZE
            "словосочетание" -> wordCount == COLLOCATION_SIZE
            "выражение" -> wordCount >= EXPRESSION_SIZE
            else -> false
        }
    }

    private fun isWordInDictionary(latinInput: String): Boolean {

        val currentDictionary = loadDictionary()
        return currentDictionary.any { it.original.equals(latinInput, ignoreCase = true) }
    }

    private fun checkLatinWords(latinInput: String): Boolean {

        val inputSplit = latinInput.split(" ")
        return inputSplit[FIRST_INDEX].all { char ->
            (char in FIRST_EN_SMALL_CHAR..LAST_EN_SMALL_CHAR) ||
                    (char in FIRST_EN_BIG_CHAR..LAST_EN_BIG_CHAR)
        }
    }

    private fun checkCyrillicWords(cyrillicInput: String): Boolean {

        val inputSplit = cyrillicInput.split(" ")
        return inputSplit[FIRST_INDEX].all { char ->
            (char in FIRST_RU_SMALL_CHAR..LAST_RU_SMALL_CHAR) ||
                    (char in FIRST_RU_BIG_CHAR..LAST_RU_BIG_CHAR)
        }
    }

    fun saveDictionary(dictionary: List<Word>) {

        val wordsFile = File(WORDS_FILE)
        wordsFile.writeText("")

        dictionary.forEach { word ->
            wordsFile.appendText("${word.original}|${word.translation}|${word.type}|" +
                    "${word.correctAnswersCount}|${word.usageCount}\n")
        }
    }

    fun loadDictionary(): List<Word> {

        val dictionary = mutableListOf<Word>()
        val wordsFile = File(WORDS_FILE)

        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
            println("Словарь пуст. Пожалуйста, добавьте слова.")
            return dictionary
        }

        try {
            wordsFile.forEachLine { line ->
                val parts = line.split("|")
                if (parts.size == NUMBER_OF_COLUMNS) {
                    val original = parts[FIRST_INDEX].trim()
                    val translations = parts[SECOND_INDEX].trim()
                    val type = parts[THIRD_INDEX].trim()
                    val correctAnswersCount = parts[FOURTH_INDEX].toShortOrNull() ?: START_CORRECT_ANSWERS_COUNT
                    val usageCount = parts[FIFTH_INDEX].toIntOrNull() ?: START_USAGE_COUNT

                    dictionary.add(Word(original, translations, type, correctAnswersCount, usageCount.toShort()))
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Ошибка: ${e.message}")
        }

        return dictionary
    }
}

const val FIRST_EN_SMALL_CHAR = 'a'
const val LAST_EN_SMALL_CHAR = 'z'
const val FIRST_EN_BIG_CHAR = 'A'
const val LAST_EN_BIG_CHAR = 'Z'

const val FIRST_RU_SMALL_CHAR = 'а'
const val LAST_RU_SMALL_CHAR = 'я'
const val FIRST_RU_BIG_CHAR = 'А'
const val LAST_RU_BIG_CHAR = 'Я'

const val FIRST_INDEX = 0
const val SECOND_INDEX = 1
const val THIRD_INDEX = 2
const val FOURTH_INDEX = 3
const val FIFTH_INDEX = 4

const val NUMBER_OF_COLUMNS = 5
