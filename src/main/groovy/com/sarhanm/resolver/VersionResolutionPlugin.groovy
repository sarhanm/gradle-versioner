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

        project.afterEvaluate {
            def params = project.extensions.getByName(VERSION_RESOLVER)
            def resolver = new VersionResolver(project, params)
            project.configurations.all { resolutionStrategy.eachDependency(resolver) }
        }

    }
}
