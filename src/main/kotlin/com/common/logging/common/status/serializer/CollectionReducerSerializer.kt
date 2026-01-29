package com.common.logging.common.status.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException

class CollectionReducerSerializer(
    private val limit: Int = 0
) : StdSerializer<Collection<*>>(Collection::class.java) {
    @Throws(IOException::class)
    override fun serialize(value: Collection<*>, gen: JsonGenerator, provider: SerializerProvider?) {
        gen.writeStartArray()
        var i = 0
        for (item in value) {
            if (i >= limit) {
                break
            }
            gen.writeObject(item)
            i++
        }
        gen.writeEndArray()
    }
}
