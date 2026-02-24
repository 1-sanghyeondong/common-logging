package com.common.logging.common.status.serializer

import com.common.logging.common.domain.MaskType
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class MaskingSerializer(private val maskType: MaskType) : StdSerializer<String>(String::class.java) {
    override fun serialize(value: String?, gen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }
        gen.writeString(maskType.mask(value))
    }
}
