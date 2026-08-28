package com.utils.calc.core.data.local

import kotlinx.serialization.json.Json

internal val LocalJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
