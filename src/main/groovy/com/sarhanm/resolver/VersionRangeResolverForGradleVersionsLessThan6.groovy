/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver

import org.gradle.api.artifacts.ComponentMetadata
import org.gradle.api.artifacts.ComponentSelection
import org.gradle.model.Mutate

class VersionRangeResolverForGradleVersionsLessThan6 {


    VersionResolverInternal resolver

    VersionRangeResolverForGradleVersionsLessThan6(VersionResolverInternal resolver) {
        this.resolver = resolver
    }

    @Mutate
    void evaluateVersionRange(ComponentSelection selection, ComponentMetadata metadata) {

        VersionRangeResolverHelper.evaluateVersionRange(selection, metadata, resolver)
    }
}
