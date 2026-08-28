package com.utils.calc.core.data.fake

import com.utils.calc.core.domain.service.TimeSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeSource @Inject constructor() : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
