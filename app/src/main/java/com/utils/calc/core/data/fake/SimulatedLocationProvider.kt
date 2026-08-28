package com.utils.calc.core.data.fake

import com.utils.calc.core.domain.model.GeoPoint
import com.utils.calc.core.domain.service.LocationProvider
import com.utils.calc.core.domain.service.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Deriva de um ponto fixo com pequenas variacoes, como se a usuaria estivesse
 * caminhando. Nao consulta GPS nem rede. A troca por FusedLocationProvider e' a
 * Fase 3 e nao muda nada acima desta classe.
 */
@Singleton
class SimulatedLocationProvider @Inject constructor(
    private val timeSource: TimeSource,
) : LocationProvider {

    private var latitude = BASE_LATITUDE
    private var longitude = BASE_LONGITUDE

    override fun stream(): Flow<GeoPoint> = flow {
        emit(next())
        while (true) {
            delay(UPDATE_INTERVAL_MILLIS)
            emit(next())
        }
    }

    override suspend fun current(): GeoPoint = next()

    private fun next(): GeoPoint {
        latitude += Random.nextDouble(-DRIFT, DRIFT)
        longitude += Random.nextDouble(-DRIFT, DRIFT)
        return GeoPoint(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = Random.nextDouble(6.0, 22.0).toFloat(),
            capturedAt = timeSource.nowMillis(),
        )
    }

    private companion object {
        const val BASE_LATITUDE = -23.55052
        const val BASE_LONGITUDE = -46.63331
        const val DRIFT = 0.00035
        const val UPDATE_INTERVAL_MILLIS = 4_000L
    }
}
