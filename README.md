# kotlin-source-dependency-gradle-plugin

## DEPRECATED: In favour of [kproject](https://github.com/korlibs/kproject)

> Note, that this is an experimental plugin. I believe this feature should be available out of the box in Kotlin. So please, vote this issue: <https://youtrack.jetbrains.com/issue/KT-44251>

Plugin to include Kotlin multiplatform source code from other repos without having to deploy artifacts

<https://plugins.gradle.org/plugin/com.soywiz.korlibs.kotlin-source-dependency-gradle-plugin>

```
plugins {
    id("com.soywiz.korlibs.kotlin-source-dependency-gradle-plugin") version "0.1.1"
}
```

Example of usage:

```
sourceDependencies {
    source("https://github.com/korlibs/korlibs-bundle-source-extensions.git::korma-rectangle-experimental-ext::696a97640bb93a66f07ca008cca84b1ae4013e57##d2d9e3eb8f9f8eb5c137e847677eb8b3e9038c30d1f4457d1bd05cafc5c3f251")
    source("https://github.com/korlibs/korlibs-bundle-source-extensions.git::suspend-test::ca84b8ffdd88ca64aaf6768cf574cd7aeee997d7##a7053fbb2eddacd7d3e5488fe29d0c0f43b084ca52175dd1afd6d1243d4062c3")
}
```

For JVM-only projects, this will include the `src/commonMain/kotlin` and `src/jvmMain/kotlin` folders
For multiplatform projects, this will include `src/commonMain/kotlin`, `src/jvmMain/kotlin`, `src/jsMain/kotlin`, `src/mingwX64Main/kotlin`, ... and several common, like [described here](https://github.com/korlibs/kotlin-source-dependency-gradle-plugin/blob/e5c445e7a8aa68bebe5173113beef8b7ed472af9/src/main/kotlin/com/soywiz/korlibs/SourceDependencies.kt#L169-L179).

## URLs

### GIT repository + folder

```
sourceDependencies {
    source("0️⃣https://github.com/korlibs/korge-bundles.git1️⃣::korge-admob2️⃣::1f15b6228bfe9deeccb995aff950c04923cebee63️⃣##daf1d3ced756e412a8eb389721ccf753d8900a5cd5dd503ffef19e37e510c4e8")
}
```

* 0️⃣ Link to GIT repository (a URL you can use with `git clone URL`)
* 1️⃣ `::` and a folder inside the GIT repository or `.` for the root directory
* 2️⃣ `::` and a ref (branch, tag or commit) in the GIT repository. Normally a specific commit SHA1 hash referencing a specific commit.
* 3️⃣ `##` and a SHA256 hash of the content of that folder. That SHA256 is generated by the plugin by using the actual content of the referenced folder files. If you don't specify the `##` segment, that's a warning on the plugin, pointing to the right SHA256. And if the hash is specified and doesn't match, the gradle building fails with a mismatch.

#### Security

<https://en.wikipedia.org/wiki/SHA-1>
> Since 2005, SHA-1 has not been considered secure against well-funded opponents

Just referencing to a SHA1 commit would not be secure enough as a collision could be found.
But the extra SHA256 for the content allows to securely reference to external repositories without worrying for the content to be modified. 
In fact, a malicious attacker would need to have access to a repository, rewrite/reupload the history of the repository creating a commit with the same SHA1, that also has the same SHA256 for the content, which is impractical.

This means that a single bundle URL should load always the same content, or to not load anything.
