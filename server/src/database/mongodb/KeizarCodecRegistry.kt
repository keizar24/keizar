package org.keizar.server.database.mongodb

import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializerOrNull
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry

@Suppress("FunctionName")
fun KeizarCodecRegistry(format: StringFormat): CodecRegistry {
    return CodecRegistries.fromProviders(KotlinxSerializationCodecProvider(format))
}

class KotlinxSerializationCodecProvider(
    private val format: StringFormat,
) : CodecProvider {
    override fun <T> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        val serializer = serializerOrNull(clazz) ?: return null

        return object : Codec<T> {
            override fun encode(writer: BsonWriter, value: T, encoderContext: EncoderContext?) {
                val str = format.encodeToString(serializer, registry)
                writer.writeString(str)
            }

            override fun getEncoderClass(): Class<T> {
                return clazz
            }

            override fun decode(reader: BsonReader, decoderContext: DecoderContext?): T {
                val str =
                    reader.readString() ?: return serializer.descriptor.getElementDescriptor(0).getElementName(0) as T
                val decoded = format.decodeFromString(serializer, str)
                return clazz.cast(decoded)
            }
        }
    }
}