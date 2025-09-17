package com.skeleton.mvc.api.common.logback

import com.skeleton.mvc.api.common.logback.common.domain.LoggingFilterIgnoreUrl
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.server.PathContainer
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import org.springframework.web.util.pattern.PathPattern
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class ContentCachingWrappingFilter(
    private val enableContentCaching: Boolean,
    private val ignorePathPatterns: List<PathPattern>
) : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        if (LoggingFilterIgnoreUrl.contains(request.requestURI) || enableContentCaching.not() || isIgnoredPathPatterns(request)) {
            chain.doFilter(request, response)
        } else {
            val wrappingRequest = ContentCachingRequestWrapper(request)
            val wrappingResponse = ContentCachingResponseWrapper(response)
            try {
                chain.doFilter(wrappingRequest, wrappingResponse)
            } finally {
                wrappingResponse.copyBodyToResponse()
            }
        }
    }

    private fun isIgnoredPathPatterns(request: HttpServletRequest): Boolean {
        if (ignorePathPatterns.isEmpty()) {
            return false
        }

        return ignorePathPatterns.any { it.matches(PathContainer.parsePath("/health")) }
    }
}
