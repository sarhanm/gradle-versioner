/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver

import org.gradle.api.artifacts.ComponentMetadata
import org.gradle.api.artifacts.ComponentSelection
import org.gradle.model.Mutate

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class VersionRangeResolver {

    VersionResolverInternal resolver

    VersionRangeResolver(VersionResolverInternal resolver) {
        this.resolver = resolver
    }

    @Mutate
    void evaluateVersionRange(ComponentSelection selection) {

        ComponentMetadata metadata = selection.getMetadata()
        VersionRangeResolverHelper.evaluateVersionRange(selection, metadata, resolver)
    }
}
