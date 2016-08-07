package com.sarhanm.resolver

import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails

/**
 * This object will be put into the project's properties and can be manually used.
 *
 * In our plugin, we exclude our resolver for the versionManifest configurtion but for outside usage,
 * we add a protective layer to prevent resolution of any dependency found in the versionManifest configuration.
 * This is done to prevent infinite loops.
 *
 * @author Mohammad Sarhan ( mohammad@ ) 
 * @date 8/2/16
 */
class VersionResolver implements Action<DependencyResolveDetails> {

    private Configuration versionManifest
    private VersionResolverInternal versionResolverInternal

    VersionResolver(Configuration versionManifest, VersionResolverInternal versionResolverInternal) {
        this.versionManifest = versionManifest
        this.versionResolverInternal = versionResolverInternal
    }

    @Override
    void execute(DependencyResolveDetails dependencyResolveDetails) {

        def found = versionManifest && versionManifest.dependencies.find {
            it.group == dependencyResolveDetails.requested.group &&
                    it.name == dependencyResolveDetails.requested.name
        }

        if (!found)
            versionResolverInternal.execute(dependencyResolveDetails)
    }


}
