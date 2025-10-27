package org.example.embeddingService

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
import ai.djl.repository.zoo.Criteria
import ai.djl.training.util.ProgressBar
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.embeddingService.model.PluginEmbedding
import org.example.extractorService.model.PluginAnalysis
import java.io.File

const val BATCH_SIZE = 500

class EmbeddingService(private val parsedFile: PluginAnalysis, private val outputFile: File) {

    private val json = Json { prettyPrint = true }

    fun embedPlugin(): PluginEmbedding{
        if (outputFile.exists()){
            println("\u001B[33m[INFO]\u001B[0m  File already exists, reading...")
            return PluginEmbedding.fromJson(outputFile.readText())
        }
        val keywordEmbedding = getEmbedding(parsedFile.pluginDNA.keywords.joinToString(" "))
        val descriptionEmbedding = getEmbedding(parsedFile.pluginDNA.description ?: "")
        val methodsEmbeddings = parsedFile.pluginDNA.methods.chunked(BATCH_SIZE)
            .map { batch -> getEmbedding(batch.joinToString(" ")) }
        val averageMethodsEmbedding = calculateAverage(methodsEmbeddings)

        val pluginEmbedding = PluginEmbedding(keywordEmbedding, descriptionEmbedding, averageMethodsEmbedding)

        val parsedOutput = json.encodeToString(pluginEmbedding)
        outputFile.writeText(parsedOutput)

        return pluginEmbedding
    }

    private fun calculateAverage(methodsEmbeddings: List<FloatArray>): FloatArray {
        if (methodsEmbeddings.isEmpty()) return FloatArray(0)
        val size = methodsEmbeddings.size
        val sum = FloatArray(methodsEmbeddings[0].size){0f}
        for (v in methodsEmbeddings){
            for (i in 0 until size) sum[i] += v[i]
        }
        val count = methodsEmbeddings.size.toFloat()
        for (i in sum.indices) {
            sum[i] /= count
        }

        return sum
    }

    private fun getEmbedding(text: String): FloatArray {
        val criteria = Criteria.builder()
            .setTypes(String::class.java, FloatArray::class.java)
            .optModelUrls("djl://ai.djl.huggingface.onnxruntime/sentence-transformers/all-MiniLM-L6-v2")
            .optEngine("OnnxRuntime")
            .optTranslatorFactory(TextEmbeddingTranslatorFactory())
            .optProgress(ProgressBar())
            .build()

        criteria.loadModel().use { model ->
            model.newPredictor().use { predictor ->
                return predictor.predict(text)
            }
        }
    }
}