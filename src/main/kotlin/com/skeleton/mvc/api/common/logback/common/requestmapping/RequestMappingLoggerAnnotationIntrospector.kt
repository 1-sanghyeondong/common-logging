package com.skeleton.mvc.api.common.logback.common.requestmapping

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

        return null
    }
}
