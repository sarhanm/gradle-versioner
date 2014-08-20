package com.sarhanm.versioner

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging

/**
 *
 * @author mohammad sarhan
 */
class VersionerPlugin implements Plugin<Project>{

    def logger = Logging.getLogger(VersionerPlugin)

    @Override
    void apply(Project project) {
        def versioner = new Versioner()
        logger.info "Initial project version: $project.version"
        project.version = versioner.getVersion()
        logger.quiet "com.sarhanm.versioner: Set project version to : $project.version"
    }
}
