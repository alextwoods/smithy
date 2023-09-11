$version: "2.0"

namespace aws.endpoints

/// Marks a trait as an endpoints modifier defining trait.
///
/// The targeted trait must only be applied to service shapes,
/// must be a structure, and must have the `trait` trait.
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
    /// A list of partition special cases - endpoints for a partition that do not follow the standard patterns.
    partitionSpecialCases: PartitionSpecialCaseList,
    /// A list of regional special cases - endpoints for a region that do not follow the standard patterns.
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
    /// The pattern to use for the partition endpoint.
    @required
    endpointPattern: PartitionEndpointPattern,

    /// A list of partition endpoint special cases - partitions that do not follow the services standard patterns
    /// or are located in a region other than the partition's defaultGlobalRegion.
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

/// Marks that a services has hand written endpoint rules.
@trait(
    selector: "service",
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
structure rulesBasedEndpoints { }
