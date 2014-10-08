package com.sarhanm.versioner

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.logging.Logging

/**
 * Plugin to easily set the version of a project
 * @author mohammad sarhan
 */
class VersionerPlugin implements Plugin<Project>{

    def logger = Logging.getLogger(VersionerPlugin)

    @Override
    void apply(Project project) {

        logger.debug "Initial project $project.name version: $project.version"

        def params = null
        try
        {
            //We no longer create the extension ourselves because
            //its expected to exist before this plugin is applied(only way to get the options during initialization)
            // Refer to the README about how to use this plugin correctly.

            //project.extensions.create("versioner", VersionerOptions)
            params = project.extensions.getByType(VersionerOptions)
        }
        catch(UnknownDomainObjectException ex)
        {
            params = new VersionerOptions()
        }

        if(!params.disabled)
        {
            def versioner = new Versioner(params, project.buildFile.parentFile)
            project.version = versioner.getVersion()
            logger.quiet "Set project '$project.name' version to : $project.version"
        }
    }
}


