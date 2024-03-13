$version: "1.0"

namespace smithy.example

use smithy.rules#staticContextParams

@staticContextParams(arrayParam: {value: ["foo", null, "bar"]})
operation OperationValidArray {}

@staticContextParams(arrayParam: {value: ["foo", 2]})
operation OperationInvalidArrayMixed {}

@staticContextParams(arrayParam: {value: [1, 2]})
operation OperationInvalidArrayNumber {}
