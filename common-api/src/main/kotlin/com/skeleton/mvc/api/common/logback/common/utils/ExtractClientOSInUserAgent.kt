package com.skeleton.mvc.api.common.logback.common.utils

import com.skeleton.mvc.api.common.logback.common.domain.ClientOS
import com.skeleton.mvc.api.common.logback.common.domain.OSType
import org.apache.logging.log4j.util.Strings
import org.slf4j.LoggerFactory

object ExtractClientOSInUserAgent {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private const val ANDROID_OS_PREFIX = "Linux; Android"
    private const val IOS_OS_PREFIX = "iPhone; CPU iPhone OS"

    operator fun invoke(userAgent: String?): ClientOS {
        if (userAgent.isNullOrEmpty()) {
            return ClientOS()
        }
        val extractOS =
            try {
                userAgent.split(" (")[1].split(") ")[0]
            } catch (e: Exception) {
                "-"
            }

        val versions = extractOS.trimAndSplit(";")

        val clientOS =
            when {
                versions.size < 2 -> ClientOS()

                isAndroid(extractOS) -> {
                    ClientOS(
                        osType = OSType.android,
                        osVersion = extractAndroidVersion(versions)
                    )
                }

                isIos(extractOS) -> {
                    ClientOS(
                        osType = OSType.ios,
                        osVersion = extractIosVersion(versions)
                    )
                }

                else -> ClientOS()
            }

        if (clientOS.osType == OSType.unknown) {
            logger.warn("OS 정보 파싱 실패, userAgent: $userAgent")
        }

        return clientOS
    }

    private fun extractIosVersion(versions: List<String>): String {
        val details = versions[1].trimAndSplit(" ")
        return if (details.size > 3) {
            details[3].replace('_', '.')
        } else {
            Strings.EMPTY
        }
    }

    private fun extractAndroidVersion(versions: List<String>): String {
        val details = versions[1].trimAndSplit(" ")
        return if (details.size == 2) {
            details[1].replace('_', '.')
        } else {
            Strings.EMPTY
        }
    }

    private fun isIos(os: String) = os.contains(IOS_OS_PREFIX)

    private fun isAndroid(os: String) = os.contains(ANDROID_OS_PREFIX)

    private fun String.trimAndSplit(delimiters: String): List<String> = this.trim().split(delimiters)
}
