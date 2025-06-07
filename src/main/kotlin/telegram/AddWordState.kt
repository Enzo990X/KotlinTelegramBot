package telegram

import trainer.model.Word

enum class AddWordState {
    IDLE,
    AWAITING_WORD_TYPE,
    AWAITING_ORIGINAL,
    AWAITING_TRANSLATION,
}

val userStates = mutableMapOf<Long?, AddWordState>()
val userWordData = mutableMapOf<Long?, Word>()

fun resetUserState(chatId: Long?) {
    userStates[chatId] = AddWordState.IDLE
    userWordData.remove(chatId)
}

fun getWordTypeRequirements(type: String): String {
    return when (type) {
        "слово" -> "Введите 1 слово на иностранном языке:"
        "словосочетание" -> "Введите 2 слова на иностранном языке через пробел:"
        "выражение" -> "Введите выражение на иностранном языке (3+ слов):"
        else -> "Введите текст на иностранном языке:"
    }
}
