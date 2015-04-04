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

        if (project.extensions.findByType(VersionerOptions) == null)
            project.extensions.create("versioner", VersionerOptions)
        else {
            logger.warn "WARN: Manually creating the VersionerOptions extension is no longer supported"
            logger.warn "WARN: You can define a versioner{ } block (without creating the extension) after apply this plugin"
        }

        project.afterEvaluate {
            def rootDir = project.projectDir

            logger.info "Initial project $project.name version: $project.version"

            def params = project.extensions.getByType(VersionerOptions)
            def versioner = new Versioner(params, rootDir)

            //Because we can't apply custom plugins in init.d files
            //we need a mechanism to allow for configuration to occur in the init.d files
            //before we run the versioner executions below.
            //TODO: Remove this when https://issues.gradle.org/browse/GRADLE-2407 is resolved
            if (project.properties.containsKey("versioner_setup")) {
                logger.debug "Found versioner_setup closure. Executing."
                def c = project.properties.get("versioner_setup") as Closure
                c.call(versioner, params)
            }


            if (!params.disabled) {
                project.version = versioner.getVersion()
                logger.quiet "Set project '$project.name' version to : $project.version"
            }

            //Adding git data so it can be used in the build script
            GitData data = project.extensions.create("gitdata", GitData)
            data.major = versioner.getMajorMinor()
            data.minor = versioner.getMajorMinor()
            data.point = versioner.getVersionPoint()
            data.hotfix = versioner.getHotfixNumber()
            data.branch = versioner.branch
            data.commit = versioner.commitHash
        }
    }
}


