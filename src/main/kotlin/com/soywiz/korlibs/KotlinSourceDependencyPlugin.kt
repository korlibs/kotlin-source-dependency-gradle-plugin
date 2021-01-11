package com.soywiz.korlibs

import org.gradle.api.*

open class KotlinSourceDependencyPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.extensions.add("sourceDependencies", SourceDependencies(project))
		//println("KotlinSourceDependencyPlugin")
	}
}

val Project.sourceDependencies: SourceDependencies get() = project.extensions.getByType(SourceDependencies::class.java)
fun Project.sourceDependencies(block: SourceDependencies.() -> Unit) {
	sourceDependencies.apply { block() }
}