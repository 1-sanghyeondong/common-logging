package com.common.logging.requestmapping

import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100)
class RequestMappingAspect(
    private val logger: RequestMappingLogger
) {
    @Pointcut(
        "@annotation(org.springframework.web.bind.annotation.RequestMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.GetMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PostMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PatchMapping)"
    )
    fun mappingAnnotations() {
    }

    @Pointcut("within(com.common..*)")
    fun mappingPackages() {
    }

    @Around("mappingAnnotations() && mappingPackages()")
    fun aroundRequestMapping(joinPoint: ProceedingJoinPoint): Any? {
        val request: HttpServletRequest =
            (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

        return logger.aroundRequestMapping(request, joinPoint)
    }
}
