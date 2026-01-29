package com.common.logging.requestmapping.dto

import java.util.Objects
import kotlin.collections.HashMap
import kotlin.reflect.jvm.jvmName

class MethodArgument(
    var name: String,
    var value: Any?,
    private var annotations: Array<Annotation>? = null
) {
    private val declaredAnnotations: MutableMap<String, Annotation> = HashMap()

    init {
        annotations?.let { setDeclaredAnnotations(it) }
    }

    companion object {
        fun of(
            name: String,
            value: Any?,
            annotations: Array<Annotation>?
        ): MethodArgument = MethodArgument(name, value, annotations)
    }

    private fun setDeclaredAnnotations(annotations: Array<Annotation>) {
        this.annotations = annotations
        for (annotation in annotations) {
            this.declaredAnnotations.putIfAbsent(annotation.annotationClass.jvmName, annotation)
        }
    }

    fun <T : Annotation> getAnnotation(annotationClass: Class<T>): T? {
        Objects.requireNonNull(annotationClass)
        return annotationClass.cast(declaredAnnotations[annotationClass.name])
    }
}
