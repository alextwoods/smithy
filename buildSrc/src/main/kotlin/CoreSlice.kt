import java.io.File

// Helpers for the smithy-utils-core and smithy-model-core jars, which are carved out of the class output of
// smithy-utils and smithy-model. Each core project keeps a core-classes.txt file listing the classes it packages;
// the same list drives the core jar, the exclusion from the existing jar, and the core sources and Javadoc.

/**
 * Reads a core class list: one binary class name per line (for example `a.b.Outer$Inner`).
 * Blank lines and `#` comments are ignored.
 */
fun readCoreClassList(file: File): List<String> =
    file.readLines()
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }

/** Converts binary class names to exact class file patterns, for example `a/b/Outer$Inner.class`. */
fun coreClassPatterns(classNames: List<String>): List<String> =
    classNames.map { it.replace('.', '/') + ".class" }

/** Converts binary class names to the patterns of the top-level source files that declare them. */
fun coreSourcePatterns(classNames: List<String>): List<String> =
    classNames.map { it.substringBefore('$').replace('.', '/') + ".java" }.distinct()
