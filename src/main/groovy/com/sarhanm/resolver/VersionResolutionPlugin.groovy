package com.sarhanm.resolver

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 *
 * @author mohammad sarhan
 */
class VersionResolutionPlugin implements Plugin<Project> {

    public static final String VERSION_RESOLVER = "versionResolver"
    private static final String VERSION_MANIFEST_EXT = "versionManifest"

    static final String VERSION_MANIFEST_CONFIGURATION = "versionManifest"

    @Override
    void apply(Project project) {
        def versionResolverOpt = project.extensions.create(VERSION_RESOLVER, VersionResolverOptions)
        def versionManifestOpt = versionResolverOpt.extensions.create(VERSION_MANIFEST_EXT, VersionManifestOption)

        VersionResolver resolver = VersionResolutionConfigurer.configureVersionResolution(project,
                versionManifestOpt,
                project.configurations,
                project.repositories)

        // Allow programatic access to the resolver so users can add to
        // additional plugins (ie springs dependency management plugin)
        project.extensions.add("versionResolverAction", resolver)

        project.tasks.create(name: 'outputVersionManifest',
                description: 'Outputs the version manifest of all the resolved versions.',
                type: VersionManifestOutputTask) { VersionManifestOutputTask t ->
            t.outputFile = project.file("$project.buildDir/version-manifest.yaml")
            t.versionResolver = resolver
        }

    }
}
