package org.example.extractorService.utils

import org.example.extractorService.model.ContentSignature
import java.security.MessageDigest

class HashBuilder {

    fun buildContentSignature(
        allFileHashes: Map<String, String>,
        pluginXmlContent: String?
    ): ContentSignature {
        // Find plugin.xml hash
        val pluginXmlHash = if (pluginXmlContent != null) {
            sha256(pluginXmlContent.toByteArray())
        } else {
            null
        }

        // Create a combined hash of all .class files
        val classFileHashes = allFileHashes
            .filter { it.key.contains(".jar") } // JARs contain classes
            .values
            .sorted() // Sort for consistency
            .joinToString("")

        val classFilesHash = if (classFileHashes.isNotEmpty()) {
            sha256(classFileHashes.toByteArray())
        } else {
            ""
        }

        // Create overall hash from all file hashes combined
        val combinedHashes = allFileHashes.values
            .sorted()
            .joinToString("")
        val overallHash = sha256(combinedHashes.toByteArray())

        return ContentSignature(
            overallHash = overallHash,
            pluginXmlHash = pluginXmlHash,
            classFilesHash = classFilesHash,
            fileHashes = allFileHashes
        )
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
