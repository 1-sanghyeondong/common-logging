package com.common.logging.common.requestmapping

import com.common.logging.annotations.Mask
import com.common.logging.common.status.serializer.MaskingSerializer
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.AnnotatedClass
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector

class RequestMappingLoggerAnnotationIntrospector : JacksonAnnotationIntrospector() {
    override fun isIgnorableType(annotatedClass: AnnotatedClass): Boolean? {
        val superIgnorableType = super.isIgnorableType(annotatedClass) ?: return null
        if (superIgnorableType) {
            return true
        }
        return false
    }

    override fun findSerializer(annotated: Annotated?): Any? {
        val serializeClass = super.findSerializer(annotated)
        if (serializeClass != null) {
            return serializeClass
        }
        if (annotated == null) {
            return null
        }

        // @Mask 어노테이션이 붙어있으면 해당 MaskType 의 MaskingSerializer 를 반환
        val mask = annotated.getAnnotation(Mask::class.java)
        if (mask != null) {
            return MaskingSerializer(mask.type)
        }

        return null
    }
}

