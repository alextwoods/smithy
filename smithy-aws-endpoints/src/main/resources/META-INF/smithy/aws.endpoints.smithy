$version: "2.0"

namespace aws.endpoints

/// Marks a trait as an endpoints modifier defining trait.
///
/// The targeted trait must only be applied to service shapes or operation
/// shapes, must be a structure, and must have the `trait` trait.
@trait(
    selector: "structure[trait|trait]",
    breakingChanges: [{change: "presence"}]
)
structure endpointsModifier { }

/// Marks that a services endpoints should be resolved using
/// standard regional endpoint patterns.
@trait(
    selector: "service",
    conflicts: [nonRegionalizedEndpoints],
    breakingChanges: [{change: "remove"}]
)
@endpointsModifier
structure standardRegionalEndpoints {
    partitionSpecialCases: PartitionSpecialCaseList,
    regionSpecialCases: RegionSpecialCaseList
}

@private
list PartitionSpecialCaseList {
    member: PartitionSpecialCase
}

@private
structure PartitionSpecialCase {
    @required
    partition: String,

    @required
    endpoint: String,

    dualStack: Boolean,
    fips: Boolean
}

@private
list RegionSpecialCaseList {
    member: RegionSpecialCase
}

@private
structure RegionSpecialCase {
    @required
    region: String,

    @required
    endpoint: String,

    dualStack: Boolean,
    fips: Boolean,
    signingRegion: String
}

/// Marks that a services is non-regionalized and has
/// a single endpoint in each partition.
@trait(
    selector: "service",
    conflicts: [standardRegionalEndpoints],
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
structure nonRegionalizedEndpoints {
    @required
    endpointPattern: PartitionEndpointPattern,

    partitionEndpointSpecialCases: PartitionEndpointSpecialCaseList,
}

@private
enum PartitionEndpointPattern {
    SERVICE_DNSSUFFIX = "service_dnsSuffix"
    SERVICE_REGION_DNSSUFFIX = "service_region_dnsSuffix"
}

@private
list PartitionEndpointSpecialCaseList {
    member: PartitionEndpointSpecialCase
}

@private
structure PartitionEndpointSpecialCase {
    @required
    partition: String,

    endpoint: String,
    region: String
}

/// Marks that a services has only dualStack endpoints.
@trait(
    selector: "service",
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
structure dualStackOnlyEndpoints { }