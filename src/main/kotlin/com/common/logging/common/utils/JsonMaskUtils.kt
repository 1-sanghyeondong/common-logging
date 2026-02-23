package com.common.logging.common.utils

import com.common.logging.annotations.MaskType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * 이미 직렬화된 JSON 트리에서 개인정보 필드를 재귀적으로 마스킹하는 유틸리티
 *
 * **사용 맥락**
 * - [StatusLoggingHandlerInterceptor]의 `generateErrorResponse` / `generateFailoverRequest` 처럼
 *   응답이 이미 JSON String으로 직렬화된 경우에는 `@Mask` 어노테이션을 읽을 수 없음
 * - 이 유틸리티는 [MaskType.fieldNameIndex]에 등록된 기본 필드명을 기준으로 JSON 트리를
 *   순회하며 해당 필드 값을 마스킹
 *
 * **마스킹 규칙**
 * 1. ObjectNode: 필드명이 [MaskType.fieldNameIndex]에 존재하고 값이 문자열이면 마스킹
 * 2. ObjectNode / ArrayNode: 중첩 노드에 대해 재귀 처리
 * 3. 이미 직렬화된 JSON이므로 원본 객체는 변경되지 않음 (in-place 수정)
 */
object JsonMaskUtils {

    /**
     * [node]를 in-place로 순회하며 개인정보 필드의 값을 마스킹
     *
     * @param node 마스킹을 적용할 루트 JsonNode (ObjectNode 또는 ArrayNode 권장)
     * @return 마스킹이 적용된 동일 노드 (편의를 위해 반환)
     */
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
