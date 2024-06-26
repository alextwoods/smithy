// 1 (change)
// 2 (change)
$version: "2.0" // 3 (change)

// 4 (change)
// 5 (change)
metadata x = 123 // 6 (change)

// 7 (change)
metadata y = [
    // 8 (change)
    123
]

// 9 (change)
namespace smithy.example

// 10 (change)
use smithy.api#Integer

// 11 (change)
use smithy.api#String

/// 12 (keep)
@deprecated
// 13 (change)
structure Foo {
    /// 14 (keep)
    @length(
        // 15 (change)
        min: 1
    )
    // 16 (change)
    @since("1.x")
    // 17 (change)
    bar: String

    /// 17a (keep)
    @length(min: 1)
    baz: String = ""

    /// 18 (TODO: Fix trailing comment after VALUE_ASSIGNMENT)

}

// 19 (change)
apply Foo @tags(["a"])

/// 20 (keep)
list Baz {
    member: Integer
}

structure Foo2 {
    foo2: String

    // 21 (change)
}
// 22 (change)
