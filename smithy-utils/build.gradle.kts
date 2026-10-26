/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module contains utility classes and interfaces for Smithy."

extra["displayName"] = "Smithy :: Utilities"
extra["moduleName"] = "software.amazon.smithy.utils"

// The classes listed in smithy-utils-core/core-classes.txt are compiled here but packaged in the
// smithy-utils-core jar. Consumers get that jar through this dependency, which is added only to the
// outgoing variants (and so the published POM and module metadata). It is not on this project's own
// classpaths, because the smithy-utils-core jar is built from this project's compile output.
val coreApi = configurations.dependencyScope("coreApi") {
    description = "Dependencies of the published smithy-utils variants that are not on its own classpaths."
}
configurations.named("apiElements") { extendsFrom(coreApi.get()) }
configurations.named("runtimeElements") { extendsFrom(coreApi.get()) }

dependencies {
    coreApi(project(":smithy-utils-core"))
}

val coreClassList = project(":smithy-utils-core").file("core-classes.txt")
tasks.jar {
    inputs.file(coreClassList).withPropertyName("coreClassList").withPathSensitivity(PathSensitivity.NONE)
    exclude(coreClassPatterns(readCoreClassList(coreClassList)))
    includeEmptyDirs = false
}
