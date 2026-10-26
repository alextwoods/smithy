/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    id("smithy.smithy-format-conventions")
    id("smithy.profiling-conventions")
    id("smithy.fuzz-test-conventions")
}

description = "This module provides the core implementation of loading, validating, " +
    "traversing, mutating, and serializing a Smithy model."

extra["displayName"] = "Smithy :: Model"
extra["moduleName"] = "software.amazon.smithy.model"

// The classes listed in smithy-model-core/core-classes.txt are compiled here but packaged in the
// smithy-model-core jar. Consumers get that jar through this dependency, which is added only to the
// outgoing variants (and so the published POM and module metadata). It is not on this project's own
// classpaths, because the smithy-model-core jar is built from this project's compile output.
val coreApi = configurations.dependencyScope("coreApi") {
    description = "Dependencies of the published smithy-model variants that are not on its own classpaths."
}
configurations.named("apiElements") { extendsFrom(coreApi.get()) }
configurations.named("runtimeElements") { extendsFrom(coreApi.get()) }

dependencies {
    api(project(":smithy-utils"))
    api(project(":smithy-jmespath"))
    coreApi(project(":smithy-model-core"))
    jmh(project(":smithy-utils"))
}

val coreClassList = project(":smithy-model-core").file("core-classes.txt")
tasks.jar {
    inputs.file(coreClassList).withPropertyName("coreClassList").withPathSensitivity(PathSensitivity.NONE)
    exclude(coreClassPatterns(readCoreClassList(coreClassList)))
    includeEmptyDirs = false
}
