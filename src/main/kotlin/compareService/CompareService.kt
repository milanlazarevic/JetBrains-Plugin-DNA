package org.example.compareService

import org.example.embeddingService.model.PluginEmbedding
import kotlin.math.sqrt

const val KEYWORD_WEIGHT = 0.2
const val DESCRIPTION_WEIGHT = 0.4
const val METHOD_WEIGHT = 0.4




class CompareService {
    fun calculateSimilarity(a: PluginEmbedding, b: PluginEmbedding): Double{
        val keywordSimilarity = cosineSimilarity(a.keywords, b.keywords)
        val description = cosineSimilarity(a.description, b.description)
        val methodSimilarity = cosineSimilarity(a.methods, b.methods)

        return KEYWORD_WEIGHT * keywordSimilarity + DESCRIPTION_WEIGHT * description + METHOD_WEIGHT * methodSimilarity
    }


    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        val dot = a.indices.sumOf { i -> (a[i] * b[i]).toDouble() }
        val normA = sqrt(a.sumOf { (it * it).toDouble() })
        val normB = sqrt(b.sumOf { (it * it).toDouble() })
        return if (normA == 0.0 || normB == 0.0) 0.0 else dot / (normA * normB)
    }

}