package com.sarhanm.versioner

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging

/**
 * Plugin to easily set the version of a project
 * @author mohammad sarhan
 */
class VersionerPlugin implements Plugin<Project>{

    def logger = Logging.getLogger(VersionerPlugin)

    @Override
    void apply(Project project) {

        logger.info "com.sarhanm.versioner: Initial project version: $project.version"

        project.extensions.create("versioner", VersionerPluginExtension)

        project.afterEvaluate {

            def VersionerPluginExtension params = project.extensions.getByName("versioner")

            def versioner = new Versioner(params.solidBranchRegex,params.solidBranchRegex)

            if(params.snapshot)
                project.version = versioner.getSnapshotVersion()
            else
                project.version = versioner.getVersion()

            logger.quiet "com.sarhanm.versioner: Set project version to : $project.version"
        }

    }
}


