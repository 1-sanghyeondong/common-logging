package com.skeleton.mvc.api.common.logback.common.utils

import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.skeleton.mvc.api.common.logback.common.requestmapping.RequestMappingLoggerAnnotationIntrospector
import com.skeleton.mvc.api.common.logback.common.status.serializer.ByteArrayReducerSerializer
import com.skeleton.mvc.api.common.logback.common.status.serializer.CollectionReducerSerializer
import com.skeleton.mvc.api.common.logback.common.status.serializer.StringReducerSerializer

object LogObjectMapper {
    val mapper = build(100, 2000, null)
    val fullBodyMapper = build(1000, 1000000, null)

    fun build(
        listMaxEntries: Int,
        stringMaxLength: Int,
        modules: List<Module>?
    ): ObjectMapper {
        val mapper = ObjectMapper().registerKotlinModule()

        mapper.setAnnotationIntrospector(
            AnnotationIntrospector.pair(
                RequestMappingLoggerAnnotationIntrospector(),
                mapper.serializationConfig.annotationIntrospector
            )
        )
        val simpleModule = SimpleModule()
        simpleModule.addSerializer(String::class.java, StringReducerSerializer(stringMaxLength))
        simpleModule.addSerializer(
            Collection::class.java,
            CollectionReducerSerializer(listMaxEntries)
        )
        simpleModule.addSerializer(ByteArray::class.java, ByteArrayReducerSerializer(stringMaxLength))
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(simpleModule)

        if (!modules.isNullOrEmpty()) {
            mapper.registerModules(modules)
        }

        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return mapper
    }
}
