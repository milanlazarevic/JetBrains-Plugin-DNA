package org.example


import org.example.compareService.CompareService
import org.example.embeddingService.EmbeddingService
import org.example.embeddingService.model.PluginEmbedding
import org.example.extractorService.ExtractorService
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage:")
        println("  extract <input.jar|input.zip> <output.txt>   - Extract entries")
        println("  compare <file1.txt> <file2.txt>              - Compare extracted entries")
        return
    }

    when (args[0]) {
        "extract" -> {
            if (args.size != 3) {
                println("Usage: extract <input.jar|input.zip> <output.txt> ${args.size}")
                return
            }
            val inputFile = File(args[1])
            val outputFile = File(args[2])
            val extractorService = ExtractorService(inputFile, outputFile)
            val parsedFile = extractorService.extractEntries()

            val embeddingPath = outputFile.parent.plus("\\embedding\\").plus(outputFile.name)
            println(embeddingPath)
            val embeddingService = EmbeddingService(parsedFile, File(embeddingPath))

            println(embeddingService.embedPlugin().toString())

            println("RADI")
        }

        "compare" -> {
            if (args.size != 3) {
                println("Usage: compare <file1.txt> <file2.txt>")
                return
            }
            val file1 = File(args[1])
            val file2 = File(args[2])
            compareFiles(file1, file2)
        }

        else -> println("Unknown command: ${args[0]}")
    }
}


/**
 * Compares two text files line by line and shows differences.
 */
fun compareFiles(file1: File, file2: File) {
    if (!file1.exists() || !file2.exists()) {
        println("Both files must exist.")
        return
    }
    val embeddingA = PluginEmbedding.fromJson(file1.readText())
    val embeddingB = PluginEmbedding.fromJson(file2.readText())

    val compareService = CompareService()

    val result = compareService.calculateSimilarity(embeddingA, embeddingB)

    println("Calculated result is: $result")


}
