package com.sarhanm.resolver

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 *
 * @author mohammad sarhan
 */
class VersionResolutionPlugin implements Plugin<Project>{

    public static final String VERSION_RESOLVER = "versionResolver"
    private static final String VERSION_MANIFEST_EXT = "versionManifest"

    private static final String VERSION_MANIFEST_CONFIGURATION = "versionManifest"

    @Override
    void apply(Project project) {
        def versionResolverOpt = project.extensions.create(VERSION_RESOLVER, VersionResolverOptions)
        def versionManifestOpt = versionResolverOpt.extensions.create(VERSION_MANIFEST_EXT, VersionManifestOption)

        project.configurations.maybeCreate(VERSION_MANIFEST_CONFIGURATION)

        def resolver = new VersionResolver(project,versionManifestOpt)

        //add our resolver to existing configuration and any new configuration added
        // in the future.
        project.configurations.all { Configuration c ->
            c.resolutionStrategy.eachDependency(resolver)
        }


        project.tasks.create(name: 'outputVersionManifest',
                description: 'Outputs the version manifest of all the resolved versions.',
                type: VersionManifestOutputTask) { VersionManifestOutputTask t ->
            t.outputFile  = project.file("$project.buildDir/version-manifest.yaml")
            t.versionResolver = resolver
        }
    }
}
