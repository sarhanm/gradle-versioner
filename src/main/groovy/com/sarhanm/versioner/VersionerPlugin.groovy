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

        def rootDir = project.projectDir
        def params = null
        def versioner = null

        logger.debug "Initial project $project.name version: $project.version"

        //Because we can't apply a plugin in init.d files
        //we need a mechanism to allow for a step to occur in the init.d files
        //before we run the versioner steps below.
        //TODO: Remove this when https://issues.gradle.org/browse/GRADLE-2407 is resolved
        if(project.properties.containsKey("versioner_setup"))
        {
            logger.debug "Found versioner_setup closure. Executing."

            def c = project.properties.get("versioner_setup") as Closure

            params = project.extensions.create("versioner",VersionerOptions)
            versioner = new Versioner(params,rootDir )
            c.call( versioner, params)
        }
        else{
            logger.debug "No setup script"
        }


        try
        {
            //We no longer create the extension ourselves because
            //its expected to exist before this plugin is applied(only way to get the options during initialization)
            // Refer to the README about how to use this plugin correctly.
            if(!params)
                params = project.extensions.getByType(VersionerOptions)
        }
        catch(UnknownDomainObjectException ex)
        {
            if(!params)
                params = new VersionerOptions()
        }

        if(!versioner)
            versioner = new Versioner(params, rootDir)

        if(!params.disabled)
        {
            project.version = versioner.getVersion()
            logger.quiet "Set project '$project.name' version to : $project.version"
        }

        //Adding git data so it can be used in the build script
        GitData data = project.extensions.create("gitdata",GitData)
        data.major = versioner.getMajorNumber()
        data.minor = versioner.getMinorNumber()
        data.point = versioner.getPointNumber()
        data.hotfix = versioner.getHotfixNumber()
        data.branch = versioner.branch
        data.commit = versioner.commitHash
    }
}


