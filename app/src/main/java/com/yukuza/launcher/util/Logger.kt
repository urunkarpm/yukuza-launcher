package com.yukuza.launcher.util

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized logging utility for structured logging across the application.
 * Supports different log levels and automatically tags logs with the class name.
 */
@Singleton
class Logger @Inject constructor() {

    private val tagMap = mutableMapOf<String, String>()

    fun d(message: String, throwable: Throwable? = null) {
        val tag = getTag()
        if (throwable != null) {
            Log.d(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }
    }

    fun i(message: String, throwable: Throwable? = null) {
        val tag = getTag()
        if (throwable != null) {
            Log.i(tag, message, throwable)
        } else {
            Log.i(tag, message)
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        val tag = getTag()
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        val tag = getTag()
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun v(message: String, throwable: Throwable? = null) {
        val tag = getTag()
        if (throwable != null) {
            Log.v(tag, message, throwable)
        } else {
            Log.v(tag, message)
        }
    }

    fun wtf(message: String, throwable: Throwable? = null) {
        val tag = getTag()
        if (throwable != null) {
            Log.wtf(tag, message, throwable)
        } else {
            Log.wtf(tag, message)
        }
    }

    private fun getTag(): String {
        val exception = Exception()
        val stackTrace = exception.stackTrace
        // Find the first non-Logger class in the stack trace
        for (element in stackTrace) {
            if (element.className != Logger::class.java.name && 
                !element.className.startsWith("androidx") &&
                !element.className.startsWith("kotlin")) {
                return tagMap.getOrPut(element.className) {
                    element.className.substringAfterLast('.').take(23)
                }
            }
        }
        return "YukuzaLauncher"
    }
}
