# kotlin-source-dependency-gradle-plugin

Plugin to include Kotlin multiplatform source code from other repos without having to deploy artifacts

<https://plugins.gradle.org/plugin/com.soywiz.korlibs.kotlin-source-dependency-gradle-plugin>

```
plugins {
  id("com.soywiz.korlibs.kotlin-source-dependency-gradle-plugin") version "0.1.0"
}
```

Example of usage:

```
sourceDependencies {
	source("https://github.com/korlibs/korlibs-bundle-source-extensions.git::korma-rectangle-experimental-ext::696a97640bb93a66f07ca008cca84b1ae4013e57##d2d9e3eb8f9f8eb5c137e847677eb8b3e9038c30d1f4457d1bd05cafc5c3f251")
}
```
