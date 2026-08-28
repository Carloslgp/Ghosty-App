package com.utils.calc.core.domain.service

import com.utils.calc.core.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    fun stream(): Flow<GeoPoint>

    suspend fun current(): GeoPoint?
}
