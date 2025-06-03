package telegram

import trainer.model.Word

enum class AddWordState {
    IDLE,
    AWAITING_WORD_TYPE,
    AWAITING_ORIGINAL,
    AWAITING_TRANSLATION,
}

val userStates = mutableMapOf<String, AddWordState>()
val userWordData = mutableMapOf<String, Word>()

fun resetUserState(chatId: String) {
    userStates[chatId] = AddWordState.IDLE
    userWordData.remove(chatId)
}

fun getWordTypeRequirements(type: String): String {
    return when (type) {
        "слово" -> "Введите 1 слово на английском:"
        "словосочетание" -> "Введите 2 слова на английском через пробел:"
        "выражение" -> "Введите выражение (3+ слов) на английском:"
        else -> "Введите текст на английском:"
    }
}
