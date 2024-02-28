package org.keizar.server.database.mongodb

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.keizar.server.database.SeedBankDbControl
import org.keizar.server.database.models.SeedBankModel

class MongoSeedBankDbControl(
    private val seedBankTable: MongoCollection<SeedBankModel>
) : SeedBankDbControl {
    override suspend fun addSeed(userId: String, seed: String): Boolean {
        return seedBankTable.insertOne(SeedBankModel(userId, seed)).wasAcknowledged()
    }

    override suspend fun removeSeed(userId: String, seed: String): Boolean {
        return seedBankTable.deleteOne(
            filter = (Field("userId") eq userId) and (Field("gameSeed") eq seed)
        ).wasAcknowledged()
    }

    override suspend fun getSeeds(userId: String): List<String> {
        val list = mutableListOf<String>()
        seedBankTable.find(
            filter = Field("userId") eq userId
        ).map { it.gameSeed }.toList(list)
        return list
    }
}