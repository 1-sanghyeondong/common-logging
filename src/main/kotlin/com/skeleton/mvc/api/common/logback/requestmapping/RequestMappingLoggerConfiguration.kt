package com.skeleton.mvc.api.common.logback.requestmapping

import com.fasterxml.jackson.databind.Module
import com.skeleton.mvc.api.common.logback.annotations.EnableLogging
import com.skeleton.mvc.api.common.logback.common.utils.LogObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.ui.Model
import org.springframework.validation.BindingResult

@Configuration
@EnableAspectJAutoProxy
@ConditionalOnBean(annotation = [EnableLogging::class])
class RequestMappingLoggerConfiguration(
    @Value("\${request-mapping-logger.request-logging.excluded-classes:}")
    private val excludedClasses: String,
    @Value("\${request-mapping-logger.response-logging.max-length:512}")
    private val responseMaxLength: Int
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun requestMappingLogger(moduleProviders: List<RequestMappingJacksonModuleProvider>): RequestMappingLogger {
        val excludedClassesSet: MutableSet<Class<*>> = HashSet()
        for (clazzString in excludedClasses.split(",").toTypedArray()) {
            try {
                if (clazzString != "") {
                    excludedClassesSet.add(Class.forName(clazzString))
                }
            } catch (e: ClassNotFoundException) {
                logger.warn("$clazzString does not exist", e)
            }
        }

        excludedClassesSet.add(BindingResult::class.java)
        excludedClassesSet.add(Model::class.java)

        val modules: List<Module> =
            moduleProviders
                .flatMap { it.getModules() }

        LogObjectMapper.mapper.registerModules(modules)
        LogObjectMapper.fullBodyMapper.registerModules(modules)

        return RequestMappingLogger(
            responseMaxLength = responseMaxLength,
            excludedArgsClasses = excludedClassesSet
        )
    }

    @Bean
    fun emptyModuleProvider(): RequestMappingJacksonModuleProvider =
        object : RequestMappingJacksonModuleProvider {
            override fun getModules() = emptyList<Module>()
        }

    @Bean
    @ConditionalOnClass(name = ["org.springframework.web.bind.annotation.GetMapping"])
    fun requestMappingAspect(requestMappingLogger: RequestMappingLogger): RequestMappingAspect = RequestMappingAspect(requestMappingLogger)
}
