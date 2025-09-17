package com.skeleton.mvc.api.common.logback.common.utils

import brave.Tracing
import brave.propagation.TraceContext
import brave.propagation.TraceContextOrSamplingFlags
import java.lang.Long
import kotlin.String
import kotlin.let

object TraceUtils {
    fun setNewTraceContext(name: String) =
        Tracing.currentTracer()?.let {
            it.withSpanInScope(
                it
                    .nextSpan(
                        TraceContextOrSamplingFlags.create(
                            TraceContext
                                .newBuilder()
                                .traceId(
                                    Long.parseUnsignedLong(
                                        randomHexID(),
                                        16
                                    )
                                ).spanId(Long.parseUnsignedLong(randomHexID(), 16))
                                .build()
                        )
                    ).name(name)
                    .start()
            )
        }

    private fun randomHexID(): String =
        List(16) {
            (('a'..'e') + ('0'..'9')).random()
        }.joinToString("")
}
