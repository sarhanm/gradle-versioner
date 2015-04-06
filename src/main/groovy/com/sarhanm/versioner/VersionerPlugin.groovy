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

        def rootDir = project.projectDir

        def params = project.extensions.getByType(VersionerOptions)
        def versioner = new Versioner(params, rootDir)

        if (!params.disabled) {
            logger.info "Initial project $project.name version: $project.version"
            project.version = versioner.getVersion()
            logger.quiet "Set project '$project.name' version to : $project.version"
        }

        //Adding git data so it can be used in the build script
        GitData data = project.extensions.create("gitdata", GitData)
        data.major = versioner.getMajorNumber()
        data.minor = versioner.getMinorNumber()
        data.point = versioner.getPointNumber()
        data.hotfix = versioner.getHotfixNumber()
        data.branch = versioner.branch
        data.commit = versioner.commitHash
        data.totalCommits = Integer.parseInt(versioner.getTotalCommits())
    }
}


