package com.skeleton.mvc.api.common.logback.common.status.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException

class ByteArrayReducerSerializer(
    private val limit: Int = 0
) : StdSerializer<ByteArray>(ByteArray::class.java) {
    @Throws(IOException::class)
    override fun serialize(
        value: ByteArray,
        gen: JsonGenerator,
        provider: SerializerProvider
    ) {
        val len = value.size.coerceAtMost(limit)
        gen.writeBinary(
            provider.config.base64Variant,
            value,
            0,
            len
        )
    }
}
