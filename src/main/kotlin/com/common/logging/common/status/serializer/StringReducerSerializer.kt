package com.common.logging.common.status.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException

class StringReducerSerializer(
    private val limit: Int = 0
) : StdSerializer<String>(String::class.java) {
    @Throws(IOException::class)
    override fun serialize(value: String, gen: JsonGenerator, provider: SerializerProvider?) {
        if (limit > 0 && value.length > limit) {
            gen.writeString(value.take(limit - 1))
        } else {
            gen.writeString(value)
        }
    }
}
