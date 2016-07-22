package com.sarhanm.resolver

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

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

            //We don't want to participate in the versionManifest configuration
            // resolution. otherwise we'll get a recursive execution
            // since that configuration is used (and resolved) in VersionResolver.
            if(c.name != VERSION_MANIFEST_CONFIGURATION)
                c.resolutionStrategy.eachDependency(resolver)
        }


        project.tasks.create(name: 'outputVersionManifest',
                description: 'Outputs the version manifest of all the resolved versions.',
                type: VersionManifestOutputTask) { VersionManifestOutputTask t ->
            t.outputFile  = project.file("$project.buildDir/version-manifest.yaml")
            t.versionResolver = resolver
        }

        /**
         * Heavily borrowed from the nebula publishing library where we
         * add solidified versions to generated pom. This is important to make sure
         * that the generated artifacts are shareable with build systems that do not use gradle
         * and/or this gradle plugin.
         *
         * This versioner plugin resolves transitive versions even if they are not marked as "auto"
         * and as long as the module is defined in the version manifest. So adding the solid version
         * to the pom does not effect the version resolution applied by this plugin.
         */
        project.plugins.withType(MavenPublishPlugin) {
            project.extensions.configure(PublishingExtension, new ClosureBackedAction<>({
                publications.withType(MavenPublication).all { pub ->

                    if (versionResolverOpt.resolveGeneratedPomVersions) {

                        pub.pom.withXml { XmlProvider xml ->

                            def dependencies = xml.asNode()?.dependencies?.dependency
                            def dependencyMap = [:]

                            dependencyMap['runtime'] = project.configurations.runtime.incoming.resolutionResult.allDependencies
                            dependencyMap['test'] = project.configurations.testRuntime.incoming.resolutionResult.allDependencies - dependencyMap['runtime']

                            dependencies?.each { Node dep ->
                                def group = dep.groupId.text()
                                def name = dep.artifactId.text()
                                def scope = dep.scope.text()

                                if (scope == 'provided') {
                                    scope = 'runtime'
                                }

                                ResolvedDependencyResult resolved = dependencyMap[scope].find { DependencyResult r ->
                                    r.requested instanceof ModuleComponentSelector &&
                                            r.requested.group == group &&
                                            r.requested.module == name
                                }

                                if (!resolved) {
                                    return
                                }

                                def versionNode = dep.version
                                if (!versionNode) {
                                    dep.appendNode('version')
                                }
                                def moduleVersion = resolved.selected.moduleVersion
                                dep.groupId[0].value = moduleVersion.group
                                dep.artifactId[0].value = moduleVersion.name
                                dep.version[0].value = moduleVersion.version
                            }
                        }
                    }
                }
            }))
        }
    }
}
