/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver

import com.sarhanm.resolver.VersionManifestOption
import com.sarhanm.resolver.VersionRangeResolver
import com.sarhanm.resolver.VersionRangeResolverForGradleVersionsLessThan6
import com.sarhanm.resolver.VersionResolutionPlugin
import com.sarhanm.resolver.VersionResolverInternal
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * Public interface to configuration version resolution
 * @author Mohammad Sarhan (mohammad@)
 */
class VersionResolutionConfigurer {


    static VersionResolver configureVersionResolution(Project project,
                                      VersionManifestOption manifestOptions,
                                      ConfigurationContainer configurations,
                                      RepositoryHandler repositoryHandler) {

        final int gradleMajorVersion = getMajorVersionFromGradleVersionString(project.getGradle().getGradleVersion())

        def versionManifestConfiguration = configurations.maybeCreate("versionManifest")

        def resolverInternal = new VersionResolverInternal(project, manifestOptions, configurations, repositoryHandler)

        configurations.all { Configuration c ->

            //We don't want to participate in the versionManifest configuration
            // resolution. otherwise we'll get a recursive execution
            // since that configuration is used (and resolved) in VersionResolver.
            if (c.name != VersionResolutionPlugin.VERSION_MANIFEST_CONFIGURATION) {
                c.resolutionStrategy.eachDependency(resolverInternal)
                if (gradleMajorVersion >= 6) {
                    c.resolutionStrategy.componentSelection.all(new VersionRangeResolver(resolverInternal))
                } else {
                    c.resolutionStrategy.componentSelection.all(new VersionRangeResolverForGradleVersionsLessThan6(resolverInternal))
                }
            }
        }

        //A public facing version resolver that can be used programatically and
        // used with other plugins
        return new VersionResolver(versionManifestConfiguration, resolverInternal)
    }

    static int getMajorVersionFromGradleVersionString(final String gradleVersionString) {
        return Integer.parseInt(gradleVersionString.substring(0,gradleVersionString.indexOf('.')));
    }

}
