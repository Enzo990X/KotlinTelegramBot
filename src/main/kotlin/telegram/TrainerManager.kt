package telegram

import trainer.LearnWordsTrainer
import trainer.model.Dictionary
import trainer.model.Settings

class TrainerManager {
    private val instances = HashMap<Long, LearnWordsTrainer>()

    fun getTrainerForUser(chatId: Long): LearnWordsTrainer {
        return instances.getOrPut(chatId) {
            val dictionary = Dictionary(chatId)
            val settings = Settings(chatId).apply { loadSettings() }
            LearnWordsTrainer(dictionary, settings)
        }
    }

    fun saveAll() {
        instances.forEach { (_, trainer) ->
            trainer.dictionary.saveDictionary(trainer.currentDictionary)
            trainer.settings.saveSettings()
        }
    }
}
