package com.common.logging.common.utils

import com.common.logging.common.domain.MaskType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

object JsonMaskUtils {
    fun mask(node: JsonNode): JsonNode {
        when (node) {
            is ObjectNode -> maskObjectNode(node)
            is ArrayNode  -> node.forEach { mask(it) }
            else          -> { /* primitive — 무시 */ }
        }
        return node
    }

    private fun maskObjectNode(node: ObjectNode) {
        val fieldNames = node.fieldNames().asSequence().toList()
        for (fieldName in fieldNames) {
            val child = node.get(fieldName) ?: continue
            val maskType = MaskType.fieldNameIndex[fieldName]
            if (maskType != null && child.isTextual) {
                node.put(fieldName, maskType.mask(child.asText()))
            } else {
                mask(child)
            }
        }
    }
}
