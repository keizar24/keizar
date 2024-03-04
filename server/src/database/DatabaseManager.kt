package org.keizar.server.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecRegistries
import org.keizar.server.database.local.InMemoryGameDataControl
import org.keizar.server.database.local.InMemorySeedBankDbControl
import org.keizar.server.database.local.InMemoryUserDbControl
import org.keizar.server.database.models.GameDataModel
import org.keizar.server.database.models.SeedBankModel
import org.keizar.server.database.models.UserModel
import org.keizar.server.database.mongodb.KeizarCodecRegistry
import org.keizar.server.database.mongodb.MongoGameDataDbControl
import org.keizar.server.database.mongodb.MongoSeedBankDbControl
import org.keizar.server.database.mongodb.MongoUserDbControl
import org.keizar.server.plugins.ServerJson

interface DatabaseManager {
    val user: UserDbControl
    val seedBank: SeedBankDbControl
    val gameData: GameDataDBControl
    suspend fun initialize() {}
}

class InMemoryDatabaseManagerImpl : DatabaseManager {
    override val user: UserDbControl = InMemoryUserDbControl()
    override val seedBank: SeedBankDbControl = InMemorySeedBankDbControl()
    override val gameData: GameDataDBControl = InMemoryGameDataControl()
}

class MongoDatabaseManagerImpl(
    connection: String
) : DatabaseManager {
    private val client = MongoClient.create(MongoClientSettings.builder().apply {
        applyConnectionString(ConnectionString(connection))
        codecRegistry(
            CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                KeizarCodecRegistry(ServerJson),
            )
        )
        uuidRepresentation(UuidRepresentation.STANDARD)
    }.build())

    private val db = client.getDatabase("keizar-production")
    private val userTable = db.getCollection<UserModel>("users")
    private val seedBankTable = db.getCollection<SeedBankModel>("seeds")
    private val gameDataTable = db.getCollection<GameDataModel>("collections")

    override suspend fun initialize() {
        seedBankTable.createIndex(
            keys = Indexes.compoundIndex(Indexes.text("userId"), Indexes.text("gameSeed")),
            options = IndexOptions().unique(true)
        )
    }

    override val user: UserDbControl = MongoUserDbControl(userTable)
    override val seedBank: SeedBankDbControl = MongoSeedBankDbControl(seedBankTable)
    override val gameData: GameDataDBControl = MongoGameDataDbControl(gameDataTable)
}
