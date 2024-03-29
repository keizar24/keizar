package org.keizar.server.data.models

import org.bson.codecs.pojo.annotations.BsonId

data class UserModel(
    @BsonId
    val id: String, // Primary Key
    val username: String, // Unique Column
    val hash: String,
    val nickname: String? = null,
    val avatarUrl: String? = null,
)