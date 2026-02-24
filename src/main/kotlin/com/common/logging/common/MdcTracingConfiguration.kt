package com.common.logging.common

import brave.sampler.Sampler
import com.common.logging.annotations.EnableMDCTraceLogging
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.aop.ObservedAspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnBean(annotation = [EnableMDCTraceLogging::class])
class MdcTracingConfiguration {

    @Bean
    @ConditionalOnMissingBean(Sampler::class)
    fun braveSampler(): Sampler = Sampler.ALWAYS_SAMPLE

    @Bean
    @ConditionalOnClass(ObservedAspect::class)
    @ConditionalOnBean(ObservationRegistry::class)
    @ConditionalOnMissingBean(ObservedAspect::class)
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect =
        ObservedAspect(observationRegistry)
}
