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

        def params = project.extensions.getByType(VersionerOptions)
        def versioner = new Versioner(params, project)

        project.version = "${->versioner.toString()}"

        //Adding git data so it can be used in the build script
        project.extensions.create("gitdata", GitData, versioner)
    }
}
