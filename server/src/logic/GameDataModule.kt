package org.keizar.server.logic

import org.keizar.server.data.DatabaseManager
import org.keizar.server.data.models.GameDataModel
import org.keizar.server.data.models.dataToModel
import org.keizar.utils.communication.game.GameDataGet
import org.keizar.utils.communication.game.GameDataStore
import org.keizar.utils.communication.game.jsonElementToRoundStats
import java.util.UUID


interface GameDataModule {
    suspend fun getGameDataByUsedID(userId: UUID): List<GameDataGet>
    suspend fun addGameData(gameData: GameDataStore): String
    suspend fun removeGameData(id: UUID): Boolean
    suspend fun saveGameData(fromString: UUID): Boolean
}

class GameDataModuleImpl (
    private val database: DatabaseManager,
) : GameDataModule {
    override suspend fun getGameDataByUsedID(userId: UUID): List<GameDataGet> {
        val username = database.user.getUserById(userId.toString())?.username ?: "Unknown"
        val gameDataModels = database.gameData.getGameDataByUser(username)
        return gameDataModels.map { modelToDataGet(it) }
    }

    override suspend fun addGameData(gameData: GameDataStore): String {
        val model = dataToModel( gameData)
        database.gameData.addGameData(model)
        return model.id.toString()
    }

    override suspend fun removeGameData(id: UUID): Boolean {
        return database.gameData.removeGameData(id)
    }

    override suspend fun saveGameData(fromString: UUID): Boolean {
        return database.gameData.saveGameData(fromString)
    }

    private suspend fun modelToDataGet(model: GameDataModel): GameDataGet {
        val userName = model.userId!!
        val opponentName = model.opponentId!!
        return GameDataGet(
            userName,
            opponentName,
            model.timeStamp,
            model.gameConfiguration,
            jsonElementToRoundStats(model.round1Statistics),
            jsonElementToRoundStats(model.round2Statistics),
            model.id.toString(),
        )
    }
}