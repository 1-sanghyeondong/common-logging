package com.common.logging.common.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.codec.binary.Base32
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.FileCopyUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Base64
import java.util.zip.GZIPInputStream

object LoggingUtil {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val mapper: ObjectMapper = ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)

    private class HeaderMapType : TypeReference<Map<String, String>>()

    const val KEY_EVENT_ID = "key-event-id"
    const val KEY_TRACE_ID = "traceId"
    const val KEY_SPAN_ID = "spanId"
    const val KEY_USER_ID = "user-id"
    const val KEY_REQUEST_FROM = "request-from"
    const val KEY_IPATH = "ipath"
    const val KEY_DEVICE_ID = "device-id"
    const val KEY_CLIENT_IP = "client-ip"
    const val KEY_APP_REFERER = "key-app-referer"
    const val KEY_REFERER = "referer"
    const val KEY_SIGN_TX_ID = "sign-tx-id"
    const val KEY_PTX_ID = "pinpoint-traceid"
    const val KEY_MDC_PTX_ID = "PtxId" // pinpoint가 mdc에 넣어주는 pinpoint transaction id의 key

    const val KEY_OS = "os"
    const val KEY_OS_VER = "os-ver"

    const val EMPTY_KEY = "-"
    private val base32: Base32 = Base32()

    private val GZIP_MAGIC: ByteArray = byteArrayOf(0x1f.toByte(), 0x8b.toByte())

    /**
     *
     * 하루 단위로 고유한(거의?) 로그 번호를 생성한다.
     *
     * @return 하루 단위로 고유한(거의?) 로그 번호이다.
     */
    fun makeUniqueEventId(): String {
        // 1 day = 24 * 60 * 60 * 1,000 milli seconds = 86,400,000 milli seconds
        val uniqueTime = System.currentTimeMillis() % 86400000L * 1000000L + System.nanoTime() % 1000000L
        val buffer = ByteBuffer.allocate(8)
        buffer.putLong(uniqueTime)
        return base32.encodeToString(buffer.array()).substring(3, 13)
    }

    /**
     *
     * 30년 단위로 고유한(거의?) 로그 번호를 생성한다.
     *
     * @return 30년 단위로 고유한(거의?) 로그 번호이다.
     */
    fun makeUniqueEventIdCandidate(): String {
        // 30years = 30 * 365 * 24 * 60 * 60 * 1,000 milli seconds = 946,080,000,000 milli seconds
        var uniqueTime = System.currentTimeMillis() % 946080000000L * 1000000L
        uniqueTime += System.nanoTime() % 1000000
        val buffer = ByteBuffer.allocate(8)
        buffer.putLong(uniqueTime)
        return base32.encodeToString(buffer.array()).substring(1, 13)
    }

    /**
     *
     * DeviceId 공통포맷
     *
     * @return 13자리 deviceId
     */
    fun deviceId(deviceId: String?): String? {
        if (deviceId.isNullOrEmpty()) {
            return EMPTY_KEY
        }
        val encodedDeviceId: String = base32.encodeToString(deviceId.toByteArray())
        return if (encodedDeviceId.length <= 13) {
            encodedDeviceId
        } else {
            encodedDeviceId.substring(0, 13)
        }
    }

    fun deserializeInternalHeader(base64Enc: String): Map<String, String> =
        try {
            val base64Dec = Base64.getDecoder().decode(base64Enc)
            val result =
                if (startsWithGzipMagicNumber(base64Dec)) {
                    unzip(base64Dec)
                } else {
                    base64Dec
                }
            mapper.readValue(result.sliceArray(IntRange(0, result.size - 1)), HeaderMapType())
        } catch (e: Exception) {
            logger.warn("InternalHeader deserialize Error", e)
            mapOf()
        }

    fun startsWithGzipMagicNumber(bytes: ByteArray) = bytes.size >= 2 && bytes[0] == GZIP_MAGIC[0] && bytes[1] == GZIP_MAGIC[1]

    private fun unzip(encoded: ByteArray): ByteArray {
        try {
            val bis = ByteArrayInputStream(encoded)
            val gis = GZIPInputStream(bis)
            return FileCopyUtils.copyToByteArray(gis)
        } catch (ex: IOException) {
            throw IllegalStateException("couldn't decode body from gzip", ex)
        }
    }
}

fun <K, V> Map<K, V>.filterNotEmpty() =
    filterValues {
        if (it is String) {
            it.isNotEmpty() && (it != LoggingUtil.EMPTY_KEY)
        } else {
            true
        }
    }
