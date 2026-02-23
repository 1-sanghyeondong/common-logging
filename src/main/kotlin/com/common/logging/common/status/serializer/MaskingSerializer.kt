package com.common.logging.common.status.serializer

import com.common.logging.annotations.MaskType
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * [@Mask] 어노테이션이 선언된 String 필드에 적용되는 Jackson Serializer
 *
 * [LogObjectMapper]가 DTO를 직렬화할 때 [RequestMappingLoggerAnnotationIntrospector]에 의해
 * 자동으로 선택되며 직접 등록할 필요 없음
 */
class MaskingSerializer(private val maskType: MaskType) : StdSerializer<String>(String::class.java) {
    override fun serialize(value: String?, gen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }
        gen.writeString(maskType.mask(value))
    }
}
