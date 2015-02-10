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

        if (project.configurations.findByName('versionManifest') == null) {
            project.configurations.create('versionManifest')
        }

        project.afterEvaluate {
            def params = project."$VERSION_RESOLVER"
            params.manifest = params.versionManifest

            def versionManifest = project.configurations.findByName('versionManifest')
            def resolved = versionManifest.resolve()

            def file = null

            if( resolved && !resolved.empty){
                file = resolved.first()
            }

            def resolver = new VersionResolver(project,params, file)
            project.configurations.all { resolutionStrategy.eachDependency(resolver) }

            //Output the computed version manifest
            if(params.outputComputedManifest)
            {
                def task = project.tasks.create(name: 'outputVersionManifest',
                        description: 'Outputs the version manifest of all the resolved versions.',
                        type: VersionManifestOutputTask) {
                    outputFile  = project.file("$project.buildDir/version-manifest.yaml")
                    versionResolver = resolver
                }

                project.tasks.build.dependsOn task
            }
        }
    }
}
