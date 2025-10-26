package org.example.extractorService


import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.extractorService.model.*
import org.example.extractorService.utils.HashBuilder
import java.io.File
import java.util.zip.ZipFile
import org.jsoup.Jsoup
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

class ExtractorService(inputFile: File, private val outputFile: File) {

    private val json = Json { prettyPrint = true }
    private val hashBuilder = HashBuilder()
    private val zipFile = ZipFile(inputFile)
    private var pluginKeywords = mutableSetOf<String>()
    private var pluginMethods = mutableSetOf<String>()
    /**
     * Extracts all entries (file paths) from a ZIP or JAR and saves them to output file.
     */
    fun extractEntries(): PluginAnalysis {
        if (outputFile.exists()){
            println("File already exists, reading...")
            return PluginAnalysis.fromJson(outputFile.readText())
        }

        val jarInfos = mutableListOf<JarInfo>()
        var pluginMetadata: PluginMetadata? = null
        var pluginXmlContent: String? = null
        val allFileHashes = mutableMapOf<String, String>()

        var totalFiles = 0
        var totalSize = 0L
        val filesByType = mutableMapOf<String, Int>()

        zipFile.use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.name.endsWith(".jar")) {
                    totalFiles++
                    totalSize += entry.size

                    val extension = entry.name.substringAfterLast('.', "")
                    filesByType[extension] = filesByType.getOrDefault(extension, 0) + 1
                    // Extract JAR to temp or process in-memory
                    val jarBytes = zip.getInputStream(entry).readBytes()
                    val jarHash = hashBuilder.sha256(jarBytes)
                    allFileHashes[entry.name] = jarHash

                    val jarInfo = analyzeJar(entry.name, jarBytes)
                    jarInfos.add(jarInfo)

                    // Check if this JAR contains plugin.xml
                    if (jarInfo.hasPluginXml) {
                        pluginXmlContent = extractPluginXmlContent(jarBytes)
                        pluginMetadata = extractPluginMetadata(jarBytes)
                    }
                }
            }
        }


        // Build the complete analysis...
        val parsedFile = PluginAnalysis(
            metadata = pluginMetadata ?: PluginMetadata(null, null, null, null, null),
            structure = buildStructureInfo(jarInfos, totalFiles, totalSize, filesByType),
            dependencies = extractDependencies(jarInfos, pluginXmlContent),
            contentSignature = hashBuilder.buildContentSignature(allFileHashes, pluginXmlContent),
            pluginDNA = PluginDNA(pluginKeywords, extractPluginDescription(pluginXmlContent), pluginMethods)

        )
        val parsedOutput = json.encodeToString(parsedFile)
        outputFile.writeText(parsedOutput)
        return parsedFile

    }

    private fun extractSymbolsFromClass(classBytes: ByteArray): List<String> {
        val classReader = ClassReader(classBytes)
        val methods = mutableListOf<String>()
        classReader.accept(object : ClassVisitor(ASM9) {
            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor? {
                methods.add(name)
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }, 0)
        return methods
    }

    private fun analyzeJar(jarName: String, jarBytes: ByteArray): JarInfo {

        // Create a temporary file or use in-memory ZIP
        val tempFile = File.createTempFile("jar-analysis", ".jar")
        tempFile.writeBytes(jarBytes)
        println(jarName)

        val zipFile = ZipFile(tempFile)
        var hasPluginXml = false
        var entryCount = 0
        val packages = mutableSetOf<String>()
        val res = extractKeywords(jarName)
        pluginKeywords.addAll(res)

        zipFile.use { jar ->
            jar.entries().asSequence().forEach { entry ->
                entryCount++

                if (entry.name == "META-INF/plugin.xml") {
                    hasPluginXml = true
                }

                // Extract package from .class files
                if (entry.name.endsWith(".class")) {
                    val packagePath = entry.name.substringBeforeLast('/')
                        .replace('/', '.')
                    pluginMethods.addAll(extractSymbolsFromClass(jar.getInputStream(entry).readBytes()))
                    pluginKeywords.addAll(extractKeywords(packagePath))
                    if (packagePath.isNotEmpty()) {
                        packages.add(packagePath)
                    }
                }
            }
        }

        tempFile.delete()

        return JarInfo(
            name = jarName,
            size = jarBytes.size.toLong(),
            entryCount = entryCount,
            hasPluginXml = hasPluginXml,
            packages = packages,
        )
    }

    private fun extractKeywords(path: String): Set<String>{
       return path.split(Regex("[\\-/._]+")).
       map{it.lowercase()}.filter { it.isNotBlank() }.distinct().toSet()
    }

    private fun extractPluginXmlContent(jarBytes: ByteArray): String? {
        val tempFile = File.createTempFile("plugin-xml", ".jar")
        tempFile.writeBytes(jarBytes)

        val zipFile = ZipFile(tempFile)
        var xmlContent: String? = null

        zipFile.use { jar ->
            val pluginXmlEntry = jar.getEntry("META-INF/plugin.xml")
            if (pluginXmlEntry != null) {
                xmlContent = jar.getInputStream(pluginXmlEntry)
                    .bufferedReader()
                    .readText()
            }
        }

        tempFile.delete()
        return xmlContent
    }

    private fun extractPluginMetadata(jarBytes: ByteArray): PluginMetadata {
        val tempFile = File.createTempFile("plugin-xml", ".jar")
        tempFile.writeBytes(jarBytes)

        val zipFile = ZipFile(tempFile)
        var metadata: PluginMetadata? = null

        zipFile.use { jar ->
            val pluginXmlEntry = jar.getEntry("META-INF/plugin.xml")
            if (pluginXmlEntry != null) {
                val xmlContent = jar.getInputStream(pluginXmlEntry)
                    .bufferedReader()
                    .readText()
                metadata = parsePluginXml(xmlContent)
            }
        }

        tempFile.delete()
        return metadata ?: PluginMetadata(null, null, null, null, null)
    }

    private fun extractDependencies(jarInfos: List<JarInfo>, pluginXmlContent: String?): Dependencies {
        // Extract plugin dependencies from plugin.xml
        val pluginDeps = if (pluginXmlContent != null) {
            extractPluginDependencies(pluginXmlContent)
        } else {
            emptyList()
        }

        // Extract library JARs (exclude plugin's own JARs)
        val libraryJars = jarInfos
            .filter { !it.name.contains("aws-toolkit-jetbrains") } // Filter out plugin's own code
            .map { jar ->
                LibraryJar(
                    name = jar.name,
                    version = extractVersion(jar.name), // e.g., "2.17.2" from "jackson-core-2.17.2.jar"
                    size = jar.size,
                    hash = jar.hashCode().toString()
                )
            }

        // Extract IDEA version from plugin.xml
        val ideaVersion = if (pluginXmlContent != null) {
            extractIdeaVersion(pluginXmlContent)
        } else {
            null
        }

        return Dependencies(
            pluginDependencies = pluginDeps,
            libraryJars = libraryJars,
            ideaVersion = ideaVersion
        )
    }

    private fun extractPluginDescription(pluginXml: String?): String? {
        if (pluginXml != null){
            val descriptionRegex = """<description>\s*<!\[CDATA\[(.*?)]]>\s*</description>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val rawHtml = descriptionRegex.find(pluginXml)?.groupValues?.get(1)?.trim() ?: return null
            return Jsoup.parse(rawHtml).text()
        }else{
            return ""
        }
    }

    private fun extractIdeaVersion(pluginXml: String): IdeaVersionRange? {
        val versionRegex = """<idea-version\s+since-build="([^"]+)"(?:\s+until-build="([^"]+)")?""".toRegex()
        val match = versionRegex.find(pluginXml)
        return if (match != null) {
            IdeaVersionRange(
                sinceBuild = match.groupValues[1],
                untilBuild = match.groupValues.getOrNull(2)
            )
        } else {
            null
        }
    }

    private fun extractPluginDependencies(pluginXml: String): List<PluginDependency> {
        val dependsRegex = """<depends(?:\s+optional="(true|false)")?>([^<]+)</depends>""".toRegex()
        return dependsRegex.findAll(pluginXml).map { match ->
            PluginDependency(
                id = match.groupValues[2].trim(),
                optional = match.groupValues[1] == "true"
            )
        }.toList()
    }

    private fun extractVersion(jarName: String): String? {
        // Extract version like "2.17.2" from "jackson-core-2.17.2.jar"
        val versionRegex = """(\d+\.\d+\.\d+)""".toRegex()
        return versionRegex.find(jarName)?.value
    }

    private fun buildStructureInfo(
        jarInfos: MutableList<JarInfo>,
        totalFiles: Int,
        totalSize: Long,
        filesByType: MutableMap<String, Int>
    ): StructureInfo {
        return StructureInfo(
            totalFiles = totalFiles,
            totalSize = totalSize,
            filesByType = filesByType,
            jarFiles = jarInfos
        )
    }

    private fun parsePluginXml(xmlContent: String): PluginMetadata {
        // Basic XML parsing - you might want to use a proper XML library
        val idRegex = """<id>(.*?)</id>""".toRegex()
        val nameRegex = """<name>(.*?)</name>""".toRegex()
        val versionRegex = """<version>(.*?)</version>""".toRegex()

        return PluginMetadata(
            pluginId = idRegex.find(xmlContent)?.groupValues?.get(1),
            name = nameRegex.find(xmlContent)?.groupValues?.get(1),
            version = versionRegex.find(xmlContent)?.groupValues?.get(1),
            vendor = null,
            ideaVersion = null
        )
    }
}
