package trainer.model

import ktb.console.SETTINGS_FILE
import ktb.trainer.DEFAULT_NUMBER_OF_TRAINS
import ktb.trainer.DEFAULT_FILTER
import ktb.console.MENU_ONE
import ktb.console.MENU_TWO
import ktb.console.MENU_ZERO
import ktb.console.readInput
import java.io.File

class Settings(
    var numberOfIterations: Int = DEFAULT_NUMBER_OF_TRAINS,
    var filter: String = DEFAULT_FILTER
) {

    fun loadSettings() {

        val settingsFile = File(SETTINGS_FILE)
        if (settingsFile.exists()) {
            settingsFile.forEachLine { line ->
                val (key, value) = line.split("=", limit = 2)
                when (key.trim()) {
                    "numberOfIterations" -> numberOfIterations = value.trim().toIntOrNull() ?: DEFAULT_NUMBER_OF_TRAINS
                    "filter" -> filter = value.trim()
                }
            }
        } else {
            println("Нет сохранённых настроек. Установлены значения по умолчанию.")
            numberOfIterations = DEFAULT_NUMBER_OF_TRAINS
            filter = DEFAULT_FILTER
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

    private fun saveSettings() {

        val settingsFile = File(SETTINGS_FILE)
        settingsFile.writeText("numberOfIterations=$numberOfIterations\nfilter=$filter")
    }
}