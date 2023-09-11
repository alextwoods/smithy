.. _aws-endpoints:

===============================
AWS Declarative Endpoint Traits
===============================

This document defines AWS declarative endpoint traits.

.. _aws-endpoints-overview:

----------------------
AWS Endpoints Overview
----------------------

An endpoint is used to connect programmatically to an AWS service. An endpoint is the URL of the
entry point for an AWS web service.

Most AWS services are regional: they offer regional endpoints and the service's resources are independent
of similar resources in other regions.

.. _aws-region:

Region
    Each `region <https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/regions.html>`_
    consists of multiple availability zones within a single geographic area. Regions themselves are isolated
    and independent from other regions.

.. _aws-partition:

Partition
    AWS groups regions into
    `partitions <https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/partitions.html>`_.
    Every region is in exactly one partition and each partition has one or more regions.
    AWS commercial Regions are in the ``aws`` partition, Regions in China are in the ``aws-cn`` partition,
    and AWS GovCloud Regions are in the ``aws-us-gov`` partition.

.. smithy-trait:: aws.endpoints#endpointsModifier
.. _aws.endpoints#endpointsModifier-trait:

-----------------------------------------
``aws.endpoints#endpointsModifier`` trait
-----------------------------------------

Summary
    A meta-trait that marks a trait as an endpoint modifier.  Traits that are marked with this trait are
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

Because endpoint modification definitions are just specialized shapes, they
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
    An :ref:`endpoints modifier trait <aws.endpoints#endpointsModifier-trait>`
    that indicates that a service's endpoints should be resolved using the standard AWS regional
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
      - ``list`` of `PartitionSpecialCase object`_
      - A list of partition special cases - endpoints for a partition that do not follow the
        standard patterns.
    * - regionSpecialCases
      - ``list`` of `RegionSpecialCase object`_
      - A list of regional special cases - endpoints for a region that do not follow the
        standard patterns.

Conflicts with
    :ref:`aws.endpoints#nonRegionalizedEndpoints-trait`

Most AWS services are regionalized and are strongly encouraged to follow
the standard endpoint patterns defined above for consistency, and to
ensure that endpoints are forwards compatible, and that SDK updates are
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

``PartitionSpecialCase`` object
-------------------------------

A PartitionSpecialCase defines the endpoint pattern to apply for all regional endpoints
in the given partition. A PartitionSpecialCase object contains the following properties:

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


``RegionSpecialCase`` object
----------------------------

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

.. smithy-trait:: aws.endpoints#nonRegionalizedEndpoints
.. _aws.endpoints#nonRegionalizedEndpoints-trait:

-------------------------------------------------
``aws.endpoints#nonRegionalizedEndpoints`` trait
-------------------------------------------------

Summary
    An :ref:`endpoints modifier trait <aws.endpoints#endpointsModifier-trait>`
    that indicates that a service is
    `non-regionalized <https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/global-services.html#global-services-that-are-unique-by-partition>`_
    and a single endpoint should be resolved per partition.
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
    * - endpointPattern
      - ``string``
      - **Required** The pattern to use for the partition endpoint.  This value can be set to ``service_dnsSuffix`` to
        use the ``{service}.{dnsSuffix}`` pattern or ``service_region_dnsSuffix`` to use
        ``{service}.{region}.{dnsSuffix}``.
    * - partitionEndpointSpecialCases
      - ``list`` of `PartitionEndpointSpecialCase object`_
      - A list of partition endpoint special cases - partitions that do not follow the
        services standard patterns or are located in a region other than the partition's
        ``defaultGlobalRegion``.

Conflicts with
    :ref:`aws.endpoints#standardRegionalEndpoints-trait`

Non-regionalized (also known as "global" services) resolve a single endpoint per partition.
That single endpoint is located in the partition's ``defaultGlobalRegion``. Non-regionalized
services should follow one of two standard patterns:

- ``service_dnsSuffix``: ``{service}.{dnsSuffix}``
- ``service_region_dnsSuffix``: ``{service}.{region}.{dnsSuffix}``

The following example defines a non-regionalized service that uses ``{service}.{dnsSuffix}``:

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.endpoints#nonRegionalizedEndpoints

    @nonRegionalizedEndpoints(endpointPattern: "service_dnsSuffix")
    service MyService {
        version: "2020-04-02"
    }

Services should follow the standard patterns; however, occasionally there are special cases.
The following example defines a non-regionalized service that uses a special case pattern in
the ``aws`` partition and uses a non-standard global region in the ``aws-cn`` partition:

.. code-block:: smithy

    @nonRegionalizedEndpoints {
        endpointPattern: "service_dnsSuffix",
        partitionEndpointSpecialCases: [
            {
                partition: "aws",
                endpoint: "myservice.global.amazonaws.com"
            },
            {
                partition: "aws-cn",
                region: "cn-north-1"
            }
        ]
    }
    service MyService {
        version: "2020-04-02"
    }

``PartitionEndpointSpecialCase`` object
---------------------------------------

A PartitionEndpointSpecialCase object contains the following properties:

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
      - The special cased endpoint template.
    * - region
      - ``string``
      - Override the ``defaultGlobalRegion`` used in this partition.

.. smithy-trait:: aws.endpoints#dualStackOnlyEndpoints
.. _aws.endpoints#dualStackOnlyEndpoints-trait:

----------------------------------------------
``aws.endpoints#dualStackOnlyEndpoints`` trait
----------------------------------------------

Summary
    An :ref:`endpoints modifier trait <aws.endpoints#endpointsModifier-trait>`
    that indicates that a service has only
    `dual stack endpoints <https://docs.aws.amazon.com/general/latest/gr/rande.html#dual-stack-endpoints>`_,
    does not support IPV4 only endpoints, and should not have the ``useDualStackEndpoint`` endpoint parameter.
    Dual stack endpoints support IPV4 and IPV6.
Trait selector
    ``service``
Trait value
    Annotation trait

Adding the dualStackOnlyEndpoints to a service modifies the generation of endpoints from
:ref:`aws.endpoints#standardRegionalEndpoints-trait` or :ref:`aws.endpoints#nonRegionalizedEndpoints-trait`,
removes the ``useDualStackEndpoint`` parameter, and defaults the behavior to dual stack for
all partitions that support it.

The following example specifies a service that uses standard regional endpoint patterns and
is dual stack only:

.. code-block:: smithy

     @standardRegionalEndpoints
     @dualStackOnlyEndpoints
     service MyService {
         version: "2020-04-02"
     }

.. smithy-trait:: aws.endpoints#rulesBasedEndpoints
.. _aws.endpoints#rulesBasedEndpoints-trait:

-------------------------------------------
``aws.endpoints#rulesBasedEndpoints`` trait
-------------------------------------------

Summary
    An :ref:`endpoints modifier trait <aws.endpoints#endpointsModifier-trait>`
    that indicates that a service has hand written endpoint rules.
Trait selector
    ``service``
Trait value
    Annotation trait

Services marked with the ``rulesBasedEndpoints`` trait have hand written endpoint rules that
extend or replace their standard generated endpoint rules.  This trait marks the presence
of hand written rules, which may be added to the model by a transformer,
but does not specify their behavior.  ``rulesBasedEndpoints`` may extend the functionality of
endpoint behavior described through other :ref:`endpoints modifier traits <aws.endpoints#endpointsModifier-trait>`
by modifying the generated :ref:`EndpointRuleSet <smithy.rules#endpointRuleSet-trait>`.

The following example specifies a service that has standard regional endpoints extended with
hand written rules:

.. code-block:: smithy

     @standardRegionalEndpoints
     @rulesBasedEndpoints
     service MyService {
         version: "2020-04-02"
     }
