package com.skeleton.mvc.api.common.logback.requestmapping.dto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.web.multipart.MultipartFile
import java.util.function.BiConsumer

internal data class RequestLog(
    val path: String,
    val clientIp: String,
    val signature: String,
    val type: String,
    val arguments: ObjectNode
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun of(
            mapper: ObjectMapper,
            path: String,
            clientIp: String,
            signature: String,
            methodArguments: List<MethodArgument>,
            excludedArgsClasses: Set<Class<*>>
        ): RequestLog {
            val arguments: ObjectNode = JsonNodeFactory.instance.objectNode()

            filterArguments(
                methodArguments,
                excludedArgsClasses
            ) { name: String, argument: Any? ->
                try {
                    val jsonNode =
                        mapper.convertValue(
                            if (isMultiPartFileType(argument)) getMultipartFileArgument(argument) else argument,
                            JsonNode::class.java
                        )
                    arguments.set<JsonNode>(name, jsonNode)
                } catch (e: Exception) {
                    logger.warn("RequestLog arguments convert fail.", e)
                }
            }

            return RequestLog(
                path = path,
                clientIp = clientIp,
                signature = signature,
                arguments = arguments,
                type = this::class.java.simpleName
            )
        }

        // 파일을 로그에 남길 필요는 없다
        private fun isMultiPartFileType(argument: Any?): Boolean =
            when {
                argument is MultipartFile -> true
                argument is Array<*> && argument.isArrayOf<MultipartFile>() -> true
                else -> false
            }

        // 파일정보를 file 이름 정보로 전환한다.
        private fun getMultipartFileArgument(argument: Any?): String =
            when {
                argument is MultipartFile -> {
                    argument.originalFilename ?: "파일이름없음"
                }

                argument is Array<*> && argument.isArrayOf<MultipartFile>() -> {
                    (argument as Array<MultipartFile>).joinToString { it.originalFilename ?: "파일이름없음" }
                }

                else -> throw IllegalArgumentException("multipart file만 전환할 수 있습니다.")
            }

        private fun filterArguments(
            methodArguments: List<MethodArgument>,
            excludedArgsClasses: Set<Class<*>>,
            each: BiConsumer<String, Any?>
        ) {
            for (methodArgument in methodArguments) {
                val name: String = methodArgument.name
                val argument: Any? = getValueOrSecret(methodArgument)

                if (excludedArgsClasses.any { clazz: Class<*> ->
                        clazz.isInstance(argument)
                    }
                ) {
                    continue
                }
                each.accept(name, argument)
            }
        }

        private fun getValueOrSecret(methodArgument: MethodArgument): Any? {
            val argument: Any? = methodArgument.value
            return argument
        }
    }
}
