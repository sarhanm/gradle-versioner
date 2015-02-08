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

            params = project.extensions.getByType(VersionerOptions)
        }
        catch(UnknownDomainObjectException ex)
        {
            params = new VersionerOptions()
        }

        if(!params.disabled)
        {
            def versioner = new Versioner(params, project.projectDir)
            project.version = versioner.getVersion()

            //Adding git data so it can be used in the build script
            GitData data = project.extensions.create("gitdata",GitData)
            data.branch = versioner.branchNameProperty
            data.commit = versioner.commitHash

            //TODO: Add other pieces of git metadata that might be needed

            logger.quiet "Set project '$project.name' version to : $project.version"
        }
    }
}


