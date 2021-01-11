package com.soywiz.korlibs

import org.gradle.api.*

open class KotlinSourceDependencyPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		println("KotlinSourceDependencyPlugin")
	}
}
