package com.sarhanm.resolver

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

/**
 *
 * @author mohammad sarhan
 */
class VersionResolverPluginTest {

    @Test
    public void testPlugin()
    {
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(VersionResolutionPlugin)
    }
}
