package org.example.embeddingService.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.extractorService.model.PluginAnalysis

@Serializable
data class PluginEmbedding(
    val keywords: FloatArray,
    val description : FloatArray,
    val methods: FloatArray
){
    companion object {
        fun fromJson(jsonString: String): PluginEmbedding {
            return Json.decodeFromString(jsonString)
        }
    }
}