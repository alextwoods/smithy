/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module contains the smithy-utils classes needed by smithy-model-core. " +
    "The classes are compiled by smithy-utils and packaged here instead of in the smithy-utils jar."

extra["displayName"] = "Smithy :: Utilities :: Core"
extra["moduleName"] = "software.amazon.smithy.utils.core"

// This project has no sources of its own. It packages a fixed list of classes from the smithy-utils
// compile output, and smithy-utils leaves them out of its own jar.
evaluationDependsOn(":smithy-utils")
val utilsMain = project(":smithy-utils").the<SourceSetContainer>()["main"]
val coreClassList = file("core-classes.txt")
val coreClasses = readCoreClassList(coreClassList)

tasks {
    jar {
        inputs.file(coreClassList).withPropertyName("coreClassList").withPathSensitivity(PathSensitivity.NONE)
        from(utilsMain.output.classesDirs) {
            include(coreClassPatterns(coreClasses))
        }
        includeEmptyDirs = false
    }

    named<Jar>("sourcesJar") {
        from(utilsMain.java.sourceDirectories) {
            include(coreSourcePatterns(coreClasses))
        }
        includeEmptyDirs = false
    }

    javadoc {
        setSource(utilsMain.java.matching { include(coreSourcePatterns(coreClasses)) })
        classpath = files(utilsMain.output, utilsMain.compileClasspath)
    }
}

dependencies {
    // When smithy-utils is also in the graph, require at least the same version so the two jars match.
    constraints {
        api(project(":smithy-utils"))
    }
}
