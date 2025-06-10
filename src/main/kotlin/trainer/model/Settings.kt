package trainer.model

import trainer.DEFAULT_NUMBER_OF_TRAINS
import trainer.DEFAULT_FILTER
import console.MENU_ONE
import console.MENU_TWO
import console.MENU_ZERO
import console.readInput
import telegram.UserFileManager

class Settings(chatId: Long) {

    private val settingsFile = UserFileManager.getUserSettingsFile(chatId)
    var numberOfIterations: Int = DEFAULT_NUMBER_OF_TRAINS
    var filter: String = DEFAULT_FILTER

    init {
        loadSettings()
    }

    private fun loadSettings() {
        if (!settingsFile.exists()) {
            saveSettings()
            return
        }

        settingsFile.forEachLine { line ->
            val (key, value) = line.split("=", limit = 2)
            when (key) {
                "numberOfIterations" -> numberOfIterations = value.toIntOrNull() ?: 10
                "filter" -> filter = value
            }
        }
    }

    fun changeSettings() {

        println("Выберите параметр, который хотите изменить:")
        println("1 - Сколько слов учить за одну тренировку")
        println("2 - Тип тренировки (слова, словосочетания, выражения, всё)")
        println("0 - Выход в меню")

        var menuInput = readInput()
        val validMenuInputs = listOf(MENU_ONE, MENU_TWO, MENU_ZERO)

        while (menuInput !in validMenuInputs) {
            println("Ошибка. Введите число 1, 2 или 0.")
            menuInput = readInput()
        }

        when (menuInput) {
            MENU_ONE -> {
                println("Введите размер одной тренировки (текущее значение: $numberOfIterations):")
                val newIterations = readInput()

                numberOfIterations = newIterations
            }
            MENU_TWO -> {
                println("Введите тип тренировки (текущее значение: $filter):")

                val validFilterInputs = listOf("слова", "словосочетания", "выражения", "всё")
                var newFilterInput = readln()

                while (newFilterInput !in validFilterInputs) {
                    println("Ошибка. Введите \"слова\", \"словосочетания\", \"выражения\" или \"всё\".")
                    newFilterInput = readln()
                }

                filter = newFilterInput
            }
            MENU_ZERO -> return
        }

        saveSettings()
        println("Настройки сохранены.\n")
    }

    fun saveSettings() {
        settingsFile.writeText("numberOfIterations=$numberOfIterations\nfilter=$filter")
    }
}
