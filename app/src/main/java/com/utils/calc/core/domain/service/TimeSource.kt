package com.utils.calc.core.domain.service

fun interface TimeSource {
    fun nowMillis(): Long
}
