$version: "2.0"

namespace smithy.example

use smithy.rules#operationContextParams

structure ComplexOperationInput {
    StringValue: String
    ListValue: StringList
    Nested: NestedStructure
}

list StringList {
    member: String
}

structure NestedStructure {
    NestedList: NestedList
}

list NestedList {
    member: ListStructure
}

structure ListStructure {
    Key: String
}

@operationContextParams(
    stringParam: {path: "StringValue"}
    topLevelStringList: {path: "ListValue"}
    nestedList: {path: "Nested.NestedList[*].Key"}
)
operation OperationValidString {
    input: ComplexOperationInput
}

@operationContextParams(
    UnparsablePath: {path: "-Invalid"},
    BadLintPath: {path: "[foo] | *"}
)
operation OperationInvalidPath {
    input: ComplexOperationInput
}
