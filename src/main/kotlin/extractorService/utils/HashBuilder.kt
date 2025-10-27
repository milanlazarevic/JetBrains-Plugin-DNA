package org.example.extractorService.utils

import org.example.extractorService.model.ContentSignature
import java.security.MessageDigest

class HashBuilder {

    fun buildContentSignature(
        allFileHashes: Map<String, String>,
        pluginXmlContent: String?
    ): ContentSignature {

        val pluginXmlHash = if (pluginXmlContent != null) {
            sha256(pluginXmlContent.toByteArray())
        } else {
            null
        }


        val classFileHashes = allFileHashes
            .filter { it.key.contains(".jar") }
            .values
            .sorted()
            .joinToString("")

        val classFilesHash = if (classFileHashes.isNotEmpty()) {
            sha256(classFileHashes.toByteArray())
        } else {
            ""
        }


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
