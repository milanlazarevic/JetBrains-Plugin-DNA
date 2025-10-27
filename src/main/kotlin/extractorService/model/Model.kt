package org.example.extractorService.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Dependencies(
    val pluginDependencies: List<PluginDependency>,
    val libraryJars: List<LibraryJar>,
    val ideaVersion: IdeaVersionRange?
)
@Serializable
data class PluginDependency(
    val id: String,
    val optional: Boolean = false
)
@Serializable
data class LibraryJar(
    val name: String,
    val version: String?,
    val size: Long,
    val hash: String
)

@Serializable
data class IdeaVersionRange(
    val sinceBuild: String?,
    val untilBuild: String?
)
@Serializable
data class PluginDNA(
    val keywords: Set<String>,
    val description: String?,
    val methods: Set<String>
)

@Serializable
data class PluginAnalysis(
    val metadata: PluginMetadata,
    val structure: StructureInfo,
    val dependencies: Dependencies,
    val contentSignature: ContentSignature,
    val pluginDNA: PluginDNA
)
{
    companion object {
        fun fromJson(jsonString: String): PluginAnalysis {
            try {
                return Json.decodeFromString(jsonString)
            }catch (e : Exception){
                println(e.toString())
                throw e

            }
        }
    }
}
@Serializable
data class PluginMetadata(
    val pluginId: String?,
    val name: String?,
    val version: String?,
    val vendor: String?,
    val ideaVersion: String?
)
@Serializable
data class StructureInfo(
    val totalFiles: Int,
    val totalSize: Long,
    val filesByType: Map<String, Int>,
    val jarFiles: List<JarInfo>
)
@Serializable
data class JarInfo(
    val name: String,
    val size: Long,
    val entryCount: Int,
    val hasPluginXml: Boolean,
    val packages: Set<String>,
)
@Serializable
data class ContentSignature(
    val overallHash: String,
    val pluginXmlHash: String?,
    val classFilesHash: String, // Combined hash of all .class files
    val fileHashes: Map<String, String> // path -> hash for important files
)