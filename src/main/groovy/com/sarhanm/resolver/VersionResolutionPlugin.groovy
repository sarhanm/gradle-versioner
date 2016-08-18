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

        def manifestVersionConfig = project.configurations.maybeCreate(VERSION_MANIFEST_CONFIGURATION)

        // Allow programatic access to the resolver so users can add to
        // additional plugins (ie springs dependency management plugin)
        def resolver = new VersionResolverInternal( project,
                versionManifestOpt,
                project.configurations,
                project.repositories,
                project.parent?.subprojects)

        project.extensions.add("versionResolverAction", new VersionResolver(manifestVersionConfig, resolver))
        //add our resolver to existing configuration and any new configuration added
        // in the future.
        project.configurations.all { Configuration c ->

            //We don't want to participate in the versionManifest configuration
            // resolution. otherwise we'll get a recursive execution
            // since that configuration is used (and resolved) in VersionResolver.
            if (c.name != VERSION_MANIFEST_CONFIGURATION)
                c.resolutionStrategy.eachDependency(resolver)
        }

        project.tasks.create(name: 'outputVersionManifest',
                description: 'Outputs the version manifest of all the resolved versions.',
                type: VersionManifestOutputTask) { VersionManifestOutputTask t ->
            t.outputFile = project.file("$project.buildDir/version-manifest.yaml")
            t.versionResolver = resolver
        }

    }
}
