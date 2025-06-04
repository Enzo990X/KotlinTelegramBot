package telegram

import kotlinx.serialization.json.Json
import trainer.LearnWordsTrainer

data class TrainContext(
    val json: Json,
    val trainer: LearnWordsTrainer,
    val trainingState: TrainState,
    val onComplete: () -> Unit = {}
)
