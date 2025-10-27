package org.example


import org.example.compareService.CompareService
import org.example.embeddingService.EmbeddingService
import org.example.embeddingService.model.PluginEmbedding
import org.example.extractorService.ExtractorService
import java.io.File


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("\u001B[31m[ERROR]\u001B[0m Arguments missing try again!")
        println("\u001B[33m[INFO]\u001B[0m  Usage:")
        println("\u001B[33m[INFO]\u001B[0m  extract <input.jar|input.zip> <output.txt>   - Extract entries")
        println("\u001B[33m[INFO]\u001B[0m  compare <file1.txt> <file2.txt>              - Compare extracted entries")
        return
    }

    when (args[0]) {
        "extract" -> {
            if (args.size != 3) {
                println("\u001B[33m[INFO]\u001B[0m  Usage: extract <input.jar|input.zip> <output.txt> ${args.size}")
                return
            }
            val inputFile = File(args[1])
            val outputFile = File(args[2])
            extractFromPlugin(inputFile, outputFile)
        }

        "compare" -> {
            if (args.size != 3) {
                println("\u001B[33m[INFO]\u001B[0m  Usage: compare <file1.txt> <file2.txt>")
                return
            }
            val file1 = File(args[1])
            val file2 = File(args[2])
            compareFiles(file1, file2)
        }

        else -> println("\u001B[31m[ERROR]\u001B[0m  Unknown command: ${args[0]}")
    }
}

private fun extractFromPlugin(input: File, output: File){
    val extractorService = ExtractorService(input, output)
    println("\u001B[33m[INFO]\u001B[0m  Analyzing ${input.name} plugin file...")
    val parsedFile = extractorService.extractEntries()

    val embeddingPath = output.parent.plus("\\embedding\\").plus(output.name)
    println("\u001B[33m[INFO]\u001B[0m  Vector embeddings will be placed into: $embeddingPath")
    val embeddingService = EmbeddingService(parsedFile, File(embeddingPath))
    embeddingService.embedPlugin()

    println("\u001B[32m[SUCCESS]\u001B[0m  Plugin parsed successfully and placed at: ${output.name}!")
    println()
}

/**
 * Compares two text files line by line and shows differences.
 */
private fun compareFiles(file1: File, file2: File) {
    if (!file1.exists() || !file2.exists()) {
        println("\u001B[31m[ERROR]\u001B[0m  Both files must exist.")
        return
    }

    // Cosine similarity
    println("\u001B[33m[INFO]\u001B[0m  Calculating similarity between \u001B[36m${file1.name}\u001B[0m and \u001B[36m${file2.name}\u001B[0m")
    println("\u001B[33m[INFO]\u001B[0m  Calculating cosine similarity...")

    val embeddingA = PluginEmbedding.fromJson(file1.readText())
    val embeddingB = PluginEmbedding.fromJson(file2.readText())

    val compareService = CompareService()

    val result = compareService.calculateCosineSimilarity(embeddingA, embeddingB)

    println("\u001B[32m[SUCCESS]\u001B[0m  Cosine similarity result is: $result")

    // SuperMinHash
    println("\u001B[33m[INFO]\u001B[0m  Calculating SuperMinHash similarity...")

    val outFile1 = file1.parentFile.parentFile.resolve(file1.name)
    val outFile2 = file2.parentFile.parentFile.resolve(file2.name)

    val estimatedJaccard = compareService.calculateMinHashSimilarity(outFile1, outFile2, 2, 2048)

    println("\u001B[32m[SUCCESS]\u001B[0m  SuperMinHash Jaccard Distance result is: $estimatedJaccard")


}
