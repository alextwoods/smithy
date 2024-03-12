$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
    "version": "1.1",
    "parameters": {
        "Input": {
            "required": true,
            "documentation": "Array input used to test selectSet",
            "type": "StringArray"
        }
    },
    "rules": [
        {
            "conditions": [
                {
                    "fn": "selectSet",
                    "argv": [
                        {
                            "ref": "Input"
                        }
                    ],
                    "assign": "FilteredInput"
                }
            ],
            "rules": [
                {
                    "conditions": [
                        {
                            "fn": "not",
                            "argv": [
                                {
                                    "fn": "isSet",
                                    "argv": [
                                        {
                                            "fn": "getAttr",
                                            "argv": [
                                                {
                                                    "ref": "FilteredInput"
                                                },
                                                "[0]"
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ],
                    "error": "empty",
                    "type": "error"
                },
                {
                    "conditions": [
                        {
                            "fn": "getAttr",
                            "argv": [
                                {
                                    "ref": "FilteredInput"
                                },
                                "[1]"
                            ],
                            "assign": "TestValue"
                        }
                    ],
                    "error": "FilteredInput[1] = {TestValue}",
                    "type": "error"
                },
                {
                    "conditions": [],
                    "error": "FilteredInput[1] = empty",
                    "type": "error"
                }
            ],
            "type": "tree"
        }
    ]
})
@endpointTests(
    version: "1.0",
    testCases: [
        {
            "documentation": "empty array returns empty array",
            "params": {
                "Input": []
            },
            "expect": {
                "error": "empty"
            }
        },
        {
            "documentation": "array with only empty values returns empty array",
            "params": {
                "Input": [null, null]
            },
            "expect": {
                "error": "empty"
            }
        },
        {
            "documentation": "array with all set values returns the array",
            "params": {
                "Input": ["0", "1"]
            },
            "expect": {
                "error": "FilteredInput[1] = 1"
            }
        },
        {
            "documentation": "array with some unset values returns only set values",
            "params": {
                "Input": ["0", null, "2"]
            },
            "expect": {
                "error": "FilteredInput[1] = 2"
            }
        },
    ]
)

@clientContextParams(
    Input: {type: "stringArray", documentation: "docs"}
)
service FizzBuzz {}