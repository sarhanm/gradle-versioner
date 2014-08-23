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

        logger.debug "com.sarhanm.versioner: Initial project $project.name version: $project.version"

        project.extensions.create("versioner", VersionerOptions)

        //We can't get user options until the project is evaluated.
        project.afterEvaluate { theProject ->

            def VersionerOptions params = theProject.extensions.getByName("versioner")

            if(!params.disabled) {
                def versioner = new Versioner(params)
                theProject.version = versioner.getVersion()
                logger.quiet "com.sarhanm.versioner: Set project '$theProject.name' version to : $theProject.version"
            }
        }
    }
}


