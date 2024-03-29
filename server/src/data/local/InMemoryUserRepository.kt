package org.keizar.server.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.keizar.server.data.AbstractUserRepository
import org.keizar.server.data.models.UserModel

class InMemoryUserRepository : AbstractUserRepository() {
    private val _data: MutableList<UserModel> = mutableListOf()
    private val mutex = Mutex()
    private suspend inline fun <T> data(crossinline block: MutableList<UserModel>.() -> T): T {
        mutex.withLock { return _data.block() }
    }

    override suspend fun addUser(userModel: UserModel): Boolean {
        return data { add(userModel.sanitized()) }
    }

    override suspend fun update(
        userId: String,
        newUsername: String?,
        newNickname: String?,
        passwordHash: String?,
        avatarUrl: String?
    ): Boolean {
        return data {
            find { it.id == userId }?.let {
                remove(it)
                add(
                    it.copy(
                        username = newUsername ?: it.username,
                        nickname = newNickname ?: it.nickname,
                        hash = passwordHash ?: it.hash,
                        avatarUrl = avatarUrl ?: it.avatarUrl
                    )
                )
                true
            } ?: false
        }
    }

    override suspend fun containsUsername(username: String): Boolean {
        return data { any { it.username.equals(username, ignoreCase = true) } }
    }

    override suspend fun getUserById(userId: String): UserModel? {
        return data { find { it.id == userId } }
    }

    override suspend fun getUserByUsername(username: String): UserModel? {
        return data { find { it.username.equals(username, ignoreCase = true) } }
    }
}