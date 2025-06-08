package telegram

import trainer.LearnWordsTrainer

data class TrainContext(
    val trainer: LearnWordsTrainer,
    val trainingState: TrainState,
    val onComplete: () -> Unit = {}
)
