package org.example.compareService

import com.dynatrace.hash4j.hashing.Hashing
import com.dynatrace.hash4j.similarity.ElementHashProvider
import com.dynatrace.hash4j.similarity.SimilarityHashing
import org.example.embeddingService.model.PluginEmbedding
import org.example.extractorService.model.PluginAnalysis
import java.io.File
import java.util.function.ToLongFunction
import kotlin.math.pow
import kotlin.math.sqrt

const val KEYWORD_WEIGHT = 0.2
const val DESCRIPTION_WEIGHT = 0.4
const val METHOD_WEIGHT = 0.4




class CompareService {
    fun calculateCosineSimilarity(a: PluginEmbedding, b: PluginEmbedding): Double{
        val keywordSimilarity = cosineSimilarity(a.keywords, b.keywords)
        val description = cosineSimilarity(a.description, b.description)
        val methodSimilarity = cosineSimilarity(a.methods, b.methods)

        return KEYWORD_WEIGHT * keywordSimilarity + DESCRIPTION_WEIGHT * description + METHOD_WEIGHT * methodSimilarity
    }

    fun calculateMinHashSimilarity(outFile1: File, outFile2: File, bitsPerComponent: Int, numberOfComponents: Int ): Double {
        val plugin1 = PluginAnalysis.fromJson(outFile1.readText())
        val plugin2 = PluginAnalysis.fromJson(outFile2.readText())

        val tokenizedFile1 = tokenizePlugin(plugin1)
        val tokenizedFile2 = tokenizePlugin(plugin2)

        val similarityHashPolicy = SimilarityHashing.superMinHash(numberOfComponents, bitsPerComponent)
        val hasher =  similarityHashPolicy.createHasher()
        val hashFunc = ToLongFunction { s: String? -> Hashing.farmHashNa().hashCharsToLong(s) }

        val signature1: ByteArray = hasher.compute(ElementHashProvider.ofCollection(tokenizedFile1, hashFunc))
        val signature2: ByteArray = hasher.compute(ElementHashProvider.ofCollection(tokenizedFile2, hashFunc))

        val fraction: Double = similarityHashPolicy.getFractionOfEqualComponents(signature1, signature2)
        return calculateJaccardDistance(fraction, bitsPerComponent)
    }

    private fun calculateJaccardDistance(fraction: Double, bitsPerComponent: Int): Double{
        return (fraction - 2.0.pow(-bitsPerComponent)) / (1.0 - 2.0.pow(-bitsPerComponent))
    }

    private fun tokenizePlugin(plugin: PluginAnalysis): Set<String> {
        val tokens = mutableSetOf<String>()
        tokens += "pluginId=${plugin.metadata.pluginId}"
        tokens += "name=${plugin.metadata.name}"
        plugin.structure.jarFiles.forEach { jar ->
            tokens += "jar=${jar.name}"
            jar.packages.forEach { pkg ->
                pkg.split('.', '-', '_').forEach { word ->
                    if (word.length > 2) tokens += "pkgWord=$word"
                }
            }
        }
        plugin.dependencies.pluginDependencies.forEach {dependency ->
            tokens += "dependency=${dependency.id}"
        }
//    tokens += "plugin_hash=${plugin.contentSignature.overallHash}"
//    plugin.contentSignature.fileHashes.forEach {hash ->
//        tokens += "file_hash=${hash.value}"
//    }
        plugin.pluginDNA.keywords.forEach { word ->
            tokens += "keyword=${word}"
        }
        plugin.pluginDNA.description?.split(" ")?.forEach { word ->
            tokens += "description=${word}"
        }
        val maxMethods = 500
        plugin.pluginDNA.methods.take(maxMethods).forEach { method ->
            method.split(Regex("(?=[A-Z])")).forEach { part -> tokens += "methodWord=${part.lowercase()}" }
        }

        return tokens
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        val dot = a.indices.sumOf { i -> (a[i] * b[i]).toDouble() }
        val normA = sqrt(a.sumOf { (it * it).toDouble() })
        val normB = sqrt(b.sumOf { (it * it).toDouble() })
        return if (normA == 0.0 || normB == 0.0) 0.0 else dot / (normA * normB)
    }

}