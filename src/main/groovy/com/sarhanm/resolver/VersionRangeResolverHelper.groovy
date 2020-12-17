/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver

import org.gradle.api.artifacts.ComponentMetadata
import org.gradle.api.artifacts.ComponentSelection

/**
 * Resolving version ranges is done by rejecting all versions that are not "in" range.
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class VersionRangeResolverHelper {

    static void evaluateVersionRange(ComponentSelection selection, ComponentMetadata metadata, VersionResolverInternal resolver) {

        def verString = null
        def group = metadata.id.group
        def name = metadata.id.name

        if (resolver.isExplicitlyVersioned(group, name)) {
            verString = resolver.getExplicitVersionFromProject(group, name)
        } else {
            verString = resolver.resolveVersion(group, name, verString)
        }

        //If we can't get to a valid version, then we won't be able to restrict the version
        if (!verString)
            return

        def verRange = new VersionRange(verString)

        if (verRange.valid && !verRange.contains(selection.candidate.version))
            selection.reject("Version $selection.candidate.version is not contained within $verString")
    }
}
