package com.soywiz.korlibs.internal

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

//internal val Project.gkotlin get() = properties["kotlin"] as KotlinMultiplatformExtension
//internal val Project.gkotlin get() = properties["kotlin"] as KotlinJvmProjectExtension
internal val Project.gkotlin get() = properties["kotlin"] as KotlinProjectExtension
//internal val Project.gkotlin get() = properties["kotlin"] as KotlinSingleTargetExtension


internal val KotlinProjectExtension.targets: List<KotlinTarget> get() {
	return when (this) {
		is KotlinSingleTargetExtension -> listOf(target)
		is KotlinMultiplatformExtension -> this.targets.toList()
		else -> TODO()
	}
}

internal val KotlinTarget.isJvm get() = name in setOf("jvm")
internal val KotlinTarget.isJs get() = name in setOf("js")
internal val KotlinTarget.isAndroid get() = name in setOf("android")
internal val KotlinTarget.isJvmOrAndroid get() = isJvm || isAndroid
internal val KotlinTarget.isIos get() = name.startsWith("ios")
internal val KotlinTarget.isTvos get() = name.startsWith("tvos")
internal val KotlinTarget.isWatchos get() = name.startsWith("watchos")
internal val KotlinTarget.isMacos get() = name.startsWith("macos")
internal val KotlinTarget.isLinux get() = name.startsWith("linux")
internal val KotlinTarget.isMingw get() = name.startsWith("mingw")
internal val KotlinTarget.isNativeDesktop get() = isMingw || isLinux || isMacos
internal val KotlinTarget.isNativeMobile get() = isIos || isTvos || isWatchos
internal val KotlinTarget.isApple get() = isIos || isTvos || isWatchos || isMacos
internal val KotlinTarget.isIosTvos get() = isIos || isTvos
internal val KotlinTarget.isIosWatchos get() = isIos || isWatchos
internal val KotlinTarget.isIosTvosWatchos get() = isIos || isTvos || isWatchos
internal val KotlinTarget.isNativePosix get() = isApple || isLinux
internal val KotlinTarget.isNative get() = isNativeDesktop || isNativeMobile
