.. _aws-endpoints:

===============================
AWS Declarative Endpoint Traits
===============================

This document defines AWS Declarative endpoint traits.


.. smithy-trait:: aws.endpoints#endpointsModifier
.. _aws.endpoints#endpointsModifier-trait:

-----------------------------------------
``aws.endpoints#endpointsModifier`` trait
-----------------------------------------

Summary
    A meta-trait that marks a trait as an endpoint modifier and describes the behavior
    of endpoint resolution for services or operations.  Traits that are marked with this trait are
    applied to service shapes or operation shapes to indicate how a client can resolve
    endpoints for that service or operation.
Trait selector
    ``[trait|trait]``
Value type
    Annotation trait

The following example defines a service with ``standardRegionalEndpoints`` modified by
the hypothetical ``fooExample`` endpoint modifier.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.endpoints#endpointsModifier
    use aws.endpoints#standardRegionalEndpoints

    @endpointsModifier
    @trait(selector: "service")
    structure fooExample {}

    @standardRegionalEndpoints
    @fooExample
    service MyService {
        version: "2020-04-02"
    }

Because endpointsModifier definitions are just specialized shapes, they
can also support configuration settings.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use aws.endpoints#endpointsModifier
    use aws.endpoints#standardRegionalEndpoints

    @endpointsModifier
    @trait(selector: "service")
    structure endpointSuffix {
        suffix: String
    }

    @standardRegionalEndpoints
    @endpointSuffix(suffix="-suffix")
    service MyService {
        version: "2020-04-02"
    }



.. smithy-trait:: aws.endpoints#standardRegionalEndpoints
.. _aws.endpoints#standardRegionalEndpoints-trait:

-------------------------------------------------
``aws.endpoints#standardRegionalEndpoints`` trait
-------------------------------------------------

Summary
    Indicates that a services endpoints should be resolved using the standard regional
    patterns:

    - Default: ``{service}.{region}.{dnsSuffix}``
    - Fips: ``{service}-fips.{region}.{dnsSuffix}``
    - Dualstack: ``{service}.{region}.{dualStackDnsSuffix}``
    - Fips/Dualstack: ``{service}-fips.{region}.{dualStackDnsSuffix}``

Trait selector
    ``service``
Trait value
    A ``structure`` with the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property
      - Type
      - Description
    * - partitionSpecialCases
      - ``List<PartitionSpecialCase>`` of `PartitionSpecialCase object`_
      - A list of partition special cases - endpoints for a partition that do not follow the
        standard patterns.
    * - regionSpecialCases
      - ``List<RegionSpecialCase>`` of `RegionSpecialCase object`_
      - A list of regional special cases - endpoints for a region that do not follow the
        standard patterns.



Most AWS services are regionalized and are strongly encouraged to follow
the standard endpoint patterns defined above, both for consistency and to
ensure that endpoints are forwards compatible and that SDK updates are
not required when the service launches in a new region or partition.

The following example defines a service that uses the standard regional endpoints:

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.endpoints#standardRegionalEndpoints

    @standardRegionalEndpoints
    service MyService {
        version: "2020-04-02"
    }

While services are strongly encouraged to follow standard endpoint patterns,
there are occasional exceptions and special cases.  The following example defines
a service that use standard regional endpoints, but uses a non-standard pattern for
FIPS endpoints in US GovCloud:

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.endpoints#standardRegionalEndpoints

    @standardRegionalEndpoints{
        partitionSpecialCases: [
            {
                partition: "aws-us-gov",
                endpoint: "myservice.{region}.{dnsSuffix}",
                fips: true
            }
        ]
    }
    service MyService {
        version: "2020-04-02"
    }

---------------------------
PartitionSpecialCase object
---------------------------

A PartitionSpecialCase object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - partition
      - ``string``
      - **Required**. The partition to special case (example: "aws").
    * - endpoint
      - ``string``
      - **Required**. The special cased endpoint template.
    * - dualStack
      - ``boolean``
      - When ``true`` the special case will apply to dualstack endpoint variants.
    * - fips
      - ``boolean``
      - When ``true`` the special case will apply to fips endpoint variants.

---------------------------
RegionSpecialCase object
---------------------------

A RegionSpecialCase object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - region
      - ``string``
      - **Required**. The region to special case (example: "us-west-2").
    * - endpoint
      - ``string``
      - **Required**. The special cased endpoint template.
    * - dualStack
      - ``boolean``
      - When ``true`` the special case will apply to dualstack endpoint variants.
    * - fips
      - ``boolean``
      - When ``true`` the special case will apply to fips endpoint variants.
    * - signingRegion
      - ``string``
      - Override the signingRegion used for this region.