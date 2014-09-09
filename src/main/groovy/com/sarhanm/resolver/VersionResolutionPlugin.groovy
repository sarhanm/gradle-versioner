package com.sarhanm.resolver

import com.sarhanm.versioner.VersionerOptions
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 * @author mohammad sarhan
 */
class VersionResolutionPlugin implements Plugin<Project>{


    public static final String VERSION_RESOLVER = "versionResolver"

    @Override
    void apply(Project project) {
        project.extensions.create(VERSION_RESOLVER, VersionResolverOptions)
        project."$VERSION_RESOLVER".extensions.create("versionManifest", VersionManifestOption)

        project.afterEvaluate {
            def params = project."$VERSION_RESOLVER"
            params.manifest = params.versionManifest
            def resolver = new VersionResolver(project, params)
            project.configurations.all { resolutionStrategy.eachDependency(resolver) }
        }

    }
}
