/*
 * Copyright 2020 Q-Jam B.V. 
 *
 */
package com.qjam.c.foundations

import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.random.Random

class Identifier<T : Identifier.Tag> private constructor(private val _id: String) : Comparable<Identifier<T>> {
    interface Tag

    override fun toString(): String {
        return _id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Identifier<*>) return false

        if (_id != other._id) return false

        return true
    }

    override fun hashCode(): Int = _id.hashCode()

    override fun compareTo(other: Identifier<T>): Int = _id.compareTo(other._id)

    fun isNone(): Boolean = this == noneIdentifier

    companion object {
        private val noneIdentifier = Identifier<Tag>("AAAAAAAA" + "AAAAAAAA" + "AAAAAAAA" + "AAAAAAAA")

        @Suppress("unchecked_cast")
        fun <T : Tag> none(): Identifier<T> = noneIdentifier as Identifier<T>

        fun <T : Tag> random(): Identifier<T> {
            val data = ByteArray(24)

            val generator = Random.Default
            generator.nextBytes(data)

            return Identifier(BaseEncoding.base64Url().encode(data).toString())
        }

        fun <T : Tag> of(id: Identifier<T>): Identifier<T> =
            Identifier(id._id)

        fun <T : Tag> of(encoded: String): Identifier<T> =
            Identifier(encoded)

        fun <T : Tag> of(value: Byte): Identifier<T> {
            val data = ByteArray(1)
            data[0] = value

            return of(data)
        }

        fun <T : Tag> of(value: Short): Identifier<T> {
            val data = ByteArray(2)
            data[0] = value.toByte()
            data[1] = (value / 256).toByte()

            return of(data)
        }

        fun <T : Tag> of(value: Int): Identifier<T> {
            val data = ByteArray(4)
            data[0] = value.toByte()
            data[1] = (value shr 8).toByte()
            data[2] = (value shr 16).toByte()
            data[3] = (value shr 24).toByte()

            return of(data)
        }

        fun <T : Tag> of(value: Long): Identifier<T> {
            val data = ByteArray(8)
            data[0] = value.toByte()
            data[1] = (value shr 8).toByte()
            data[2] = (value shr 16).toByte()
            data[3] = (value shr 24).toByte()
            data[3] = (value shr 32).toByte()
            data[3] = (value shr 40).toByte()
            data[3] = (value shr 48).toByte()
            data[3] = (value shr 56).toByte()

            return of(data)
        }

        fun <T : Tag> of(value: ByteArray): Identifier<T> {
            val data = value.copyOf(24)
            return Identifier(BaseEncoding.base64Url().encode(data).toString())
        }

        @Suppress("UnstableApiUsage")
        fun <T : Tag> ofString(value: String): Identifier<T> {
            return of(Hashing.sha256().hashString(value, Charsets.UTF_8).asBytes())
        }

        fun <T : Tag> safeOf(value: String?): Identifier<T>? {
            return if (value == null) {
                null
            } else {
                var id: Identifier<T>? = null
                try {
                    id = of(value)
                } catch (e: IllegalArgumentException) {
                    //
                }

                id
            }
        }

    }
}

@ExperimentalSerializationApi
@Serializer(forClass = Identifier::class)
object IdentifierSerializer : KSerializer<Identifier<Identifier.Tag>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Identifier", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Identifier<Identifier.Tag> {
        return Identifier.of(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Identifier<Identifier.Tag>) {
        encoder.encodeString(value.toString())
    }
}
