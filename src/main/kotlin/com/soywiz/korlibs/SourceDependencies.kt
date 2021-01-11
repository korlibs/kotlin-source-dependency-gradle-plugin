package com.soywiz.korlibs

import com.soywiz.korlibs.internal.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import java.io.*
import java.net.*
import java.security.*

class SourceDependencies(val project: Project) {
    val sourcesDir get() = File(project.buildDir, "source-dependencies-all").also { it.mkdirs() }
    val logger get() = project.logger
    val sources = arrayListOf<SourceInfo>()
    data class SourceInfo(
		val path: File,
		val sourceName: String,
		val repositories: List<SourceRepository>,
		val dependencies: List<SourceDependency>
    ) {
        fun dependenciesForSourceSet(sourceSet: String) = dependencies.filter { it.sourceSet == sourceSet }
        fun dependenciesForSourceSet(sourceSet: Set<String>) = dependencies.filter { it.sourceSet in sourceSet }
    }
    data class SourceRepository(val url: String)
    data class SourceDependency(val sourceSet: String, val artifactPath: String)

    fun sha256Tree(tree: FileTree): String {
        val files = LinkedHashMap<String, File>()
        tree.visit {
            if (!it.isDirectory) {
                val mpath = it.path.trim('/')
                val rpath = "/$mpath"
                when {
                    rpath.contains("/.git") -> Unit
                    rpath.endsWith("/.DS_Store") -> Unit
                    rpath.endsWith("/thumbs.db") -> Unit
                    else -> files[mpath] = it.file
                }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        for (fileKey in files.keys.toList().sorted()) {
            val file = files[fileKey]!!
            val hashString = "$fileKey[${file.length()}]"
            digest.update(hashString.toByteArray(Charsets.UTF_8))
            digest.update(file.readBytes())
            if (logger.isInfoEnabled) {
                logger.info("SHA256: $hashString: ${(digest.clone() as MessageDigest).digest().hex}")
            }
        }
        return digest.digest().hex
    }

    val buildDirSourceFolder = "source-dependencies"

    @JvmOverloads
    fun source(zipFile: File, baseName: String? = null, checkSha256: String? = null) {
        val bundleName = baseName ?: zipFile.name.removeSuffix(".kotlinsourcezip")
        val outputDir = project.file("${project.buildDir}/$buildDirSourceFolder/$bundleName")
        if (!outputDir.exists()) {
            logger.warn("KotlinSourceDependency: Extracting $zipFile...")
            val tree = if (zipFile.isDirectory) project.fileTree(zipFile) else project.zipTree(zipFile)

            val computedSha25 = sha256Tree(tree)
            when {
                checkSha256 == null -> logger.warn("  - Security WARNING! Not checking SHA256 for bundle $bundleName. That should be SHA256 = $computedSha25")
                checkSha256 != computedSha25 -> error("Bundle '$bundleName' expects SHA256 = $checkSha256 , but found SHA256 = $computedSha25")
                else -> logger.info("Matching bundle SHA256 = $computedSha25")
            }
            //println("SHA256: ${sha256Tree(tree)}")

            project.sync {
                it.from(tree)
                it.into(outputDir)
            }
        } else {
            logger.info("KotlinSourceDependency: Already unzipped $zipFile")
        }

        val repositories = arrayListOf<SourceRepository>()
        val dependencies = arrayListOf<SourceDependency>()
        val dependenciesTxtFile = File(outputDir, "dependencies.txt")
        if (dependenciesTxtFile.exists()) {
            for (rline in dependenciesTxtFile.readLines()) {
                val line = rline.trim()
                if (line.startsWith("#")) continue
                val (key, value) = line.split(":", limit = 2).map { it.trim() }.takeIf { it.size >= 2 } ?: continue
                if (key == "repository") {
                    repositories.add(SourceRepository(value))
                } else {
                    dependencies.add(SourceDependency(key, value))
                }
            }
        }

        sources += SourceInfo(
            path = outputDir,
            sourceName = bundleName,
            repositories = repositories,
            dependencies = dependencies,
        )

        project.afterEvaluate {
            for (repo in repositories) {
                logger.info("KotlinSourceDependency.repository: $repo")
                project.repositories.maven {
                    it.url = project.uri(repo.url)
                }
            }
            for (dep in dependencies) {
                //val available = dep.sourceSet in project.configurations
                val available = try {
                    project.configurations.getAt(dep.sourceSet) != null
                } catch (e: UnknownConfigurationException) {
                    false
                }
                logger.info("KotlinSourceDependency.dependency: $dep -- available=$available")
                if (available) {
                    project.dependencies.add(dep.sourceSet, dep.artifactPath)
                }
            }
            logger.info("KotlinSourceDependency: $outputDir")

			for (sourceSet in project.gkotlin.sourceSets) {
				for ((ss, folderName) in listOf(sourceSet.resources to "resources", sourceSet.kotlin to "kotlin")) {
					val folder = File(project.buildDir, "$buildDirSourceFolder/${bundleName}/src/${sourceSet.name}/$folderName")
					if (folder.exists()) {
						logger.info("  ${ss.name}Src: $folder")
						ss.srcDirs(folder)
					} else {
						logger.info("  ${ss.name}Src: $folder (not existing)")
					}
				}
			}
            for (target in project.gkotlin.targets) {
                logger.info("  target: $target [${target.compilations.names}]")

				//println(project.gkotlin.sourceSets.names)
				//target.compilations.maybeCreate("main")
				//target.compilations.maybeCreate("test")


                target.compilations.all { compilation ->
                    logger.info("    compilation: $compilation")

                    val sourceSets = compilation.kotlinSourceSets.toMutableSet()

                    for (sourceSet in sourceSets) {
                        logger.info("      sourceSet: $sourceSet")

                        fun addSource(ss: SourceDirectorySet, sourceSetName: String, folder: String) {
                            val folder = File(project.buildDir, "$buildDirSourceFolder/${bundleName}/src/${sourceSetName}/$folder")
                            if (folder.exists()) {
                                logger.info("        ${ss.name}Src: $folder")
                                ss.srcDirs(folder)
                            } else {
								//logger.debug("        ${ss.name}Src: $folder (not existing)")
								logger.info("        ${ss.name}Src: $folder (not existing)")
							}
                        }

                        fun addSources(sourceSetName: String) {
                            addSource(sourceSet.kotlin, sourceSetName, "kotlin")
                            addSource(sourceSet.resources, sourceSetName, "resources")
                        }

                        fun addSourcesAddSuffix(sourceSetName: String) {
                            when {
                                sourceSet.name.endsWith("test", ignoreCase = true) -> addSources("${sourceSetName}Test")
                                sourceSet.name.endsWith("main", ignoreCase = true) -> addSources("${sourceSetName}Main")
                            }
                        }

						when (project.gkotlin) {
							// For JVM-only projects
							is KotlinSingleTargetExtension -> {
								addSourcesAddSuffix("common")
								if (project.gkotlin is KotlinSingleJavaTargetExtension) {
									addSourcesAddSuffix("jvm")
								}
							}
							// For multiplatform projects
							else -> {
								addSources(sourceSet.name)
								if (target.isNative) addSourcesAddSuffix("nativeCommon")
								if (target.isNativeDesktop) addSourcesAddSuffix("nativeDesktop")
								if (target.isNativePosix) addSourcesAddSuffix("nativePosix")
								if (target.isNativePosix && !target.isApple) addSourcesAddSuffix("nativePosixNonApple")
								if (target.isApple) addSourcesAddSuffix("nativePosixApple")
								if (target.isIosTvosWatchos) addSourcesAddSuffix("iosWatchosTvosCommon")
								if (target.isWatchos) addSourcesAddSuffix("iosWatchosCommon")
								if (target.isTvos) addSourcesAddSuffix("iosTvosCommon")
								if (target.isMacos || target.isIosTvos) addSourcesAddSuffix("macosIosTvosCommon")
								if (target.isMacos || target.isIosWatchos) addSourcesAddSuffix("macosIosWatchosCommon")
								if (target.isIos) addSourcesAddSuffix("iosCommon")
							}
						}
					}
                }
            }
        }
    }

    @JvmOverloads
    fun source(url: java.net.URL, baseName: String? = null, checkSha256: String? = null) {
        val outFile = File(sourcesDir, "${baseName ?: File(url.path).nameWithoutExtension}.kotlinsourcezip")
        if (!outFile.exists()) {
            logger.warn("KotlinSourceDependency: Downloading $url...")
            outFile.writeBytes(url.readBytes())
        } else {
            logger.info("KotlinSourceDependency: Already downloaded $url")
        }
		source(outFile, baseName, checkSha256)
    }

    @JvmOverloads
    fun sourceGit(repo: String, folder: String = "", ref: String = "master", bundleName: String? = null, checkSha256: String? = null) {
        val repoURL = URL(repo)
        val packPath = "${repoURL.host}/${repoURL.path}/$ref"
            .replace("\\", "/")
            .trim('/')
            .replace(Regex("/+"), "/")
            .replace(".git", "")
            .replace("/..", "")

        val packDir = File(sourcesDir, packPath)
        val packEnsure = File(sourcesDir, "$packPath.refname")
        val existsDotGitFolder = File(packDir, ".git").exists()
        val matchingReg = packEnsure.takeIf { it.exists() }?.readText() == ref

        if (!matchingReg && !existsDotGitFolder) {
            packDir.mkdirs()
            logger.warn("KotlinSourceDependency: Git cloning $repo @ $ref...")
            project.exec {
                it.workingDir(packDir)
                it.commandLine("git", "-c", "core.autocrlf=false", "clone", repo, ".")
            }.assertNormalExitValue()
        } else {
            logger.info("KotlinSourceDependency: Already cloned $repo")
        }

        if (!matchingReg) {
            project.exec {
                it.workingDir(packDir)
                it.commandLine("git", "-c", "core.autocrlf=false", "reset", "--hard", ref)
            }.assertNormalExitValue()
            project.delete {
                it.delete(File(packDir, ".git"))
            }
            packEnsure.writeText(ref)
        } else {
            logger.info("KotlinSourceDependency: Already at reference $ref @ $repo")
        }


		source(File(packDir, folder), bundleName, checkSha256)
    }

    @Suppress("unused")
	@JvmOverloads
    fun source(fullUri: String, baseName: String? = null) {
        val (uri, ssha256) = (fullUri.split("##", limit = 2) + listOf(""))
        val sha256 = ssha256.takeIf { it.isNotEmpty() }
        when {
            uri.contains(".git") -> {
                val parts = uri.split("::", limit = 3)
                sourceGit(parts[0], parts.getOrElse(1) { "" }, parts.getOrElse(2) { "master" }, parts.getOrNull(3), checkSha256 = sha256)
            }
            uri.startsWith("http://") || uri.startsWith("https://") -> {
				source(URL(uri), baseName, checkSha256 = sha256)
            }
            else -> {
				source(project.file(uri), baseName, checkSha256 = sha256)
            }
        }
    }

	fun getPaths(name: String, resources: Boolean, test: Boolean): Set<File> {
        val lfolder = if (resources) "resources" else "kotlin"
        val lmain = if (test) "Test" else "Main"
        return sources.flatMap { bundle ->
            listOf(File(bundle.path, "src/${name}$lmain/$lfolder"))
        }.filter { it.isDirectory && it.exists() }.toSet()
    }
}

