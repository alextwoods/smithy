/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module contains the minimal smithy-model classes needed to implement traits: " +
    "Trait, TraitService, ShapeId, and the Node types with JSON parsing and printing. " +
    "The classes are compiled by smithy-model and packaged here instead of in the smithy-model jar."

extra["displayName"] = "Smithy :: Model :: Core"
extra["moduleName"] = "software.amazon.smithy.model.core"

// This project has no main sources of its own. It packages a fixed list of classes from the smithy-model
// compile output, and smithy-model leaves them out of its own jar.
evaluationDependsOn(":smithy-model")
evaluationDependsOn(":smithy-utils-core")
val modelMain = project(":smithy-model").the<SourceSetContainer>()["main"]
val coreClassList = file("core-classes.txt")
val coreClasses = readCoreClassList(coreClassList)

tasks {
    jar {
        inputs.file(coreClassList).withPropertyName("coreClassList").withPathSensitivity(PathSensitivity.NONE)
        from(modelMain.output.classesDirs) {
            include(coreClassPatterns(coreClasses))
        }
        includeEmptyDirs = false
    }

    named<Jar>("sourcesJar") {
        from(modelMain.java.sourceDirectories) {
            include(coreSourcePatterns(coreClasses))
        }
        includeEmptyDirs = false
    }

    javadoc {
        setSource(modelMain.java.matching { include(coreSourcePatterns(coreClasses)) })
        classpath = files(modelMain.output, modelMain.compileClasspath)
    }

    // The core isolation guard test loads the two core jars in their own class loader, so it runs
    // against the packaged jars rather than the test runtime classpath.
    test {
        val utilsCore = project(":smithy-utils-core")
        val coreJars = files(jar, utilsCore.tasks.named("jar"))
        val classLists = files(coreClassList, utilsCore.file("core-classes.txt"))
        inputs.files(coreJars).withPropertyName("coreJars").withNormalizer(ClasspathNormalizer::class)
        inputs.files(classLists).withPropertyName("coreClassLists").withPathSensitivity(PathSensitivity.NONE)
        jvmArgumentProviders.add(
            CommandLineArgumentProvider {
                listOf(
                    "-Dsmithy.core.jars=${coreJars.asPath}",
                    "-Dsmithy.core.classLists=${classLists.asPath}",
                )
            },
        )
    }
}

dependencies {
    api(project(":smithy-utils-core"))

    // When smithy-model is also in the graph, require at least the same version so the two jars match.
    constraints {
        api(project(":smithy-model"))
    }
}
