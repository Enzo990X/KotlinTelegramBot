package telegram

import trainer.model.Question

data class TrainState(
    val chatId: String,
    var questionsRemaining: Int,
    var currentQuestion: Question? = null
)