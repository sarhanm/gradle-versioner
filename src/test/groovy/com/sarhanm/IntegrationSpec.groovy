package com.sarhanm

import com.energizedwork.spock.extensions.TempDirectory
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/**
 * @author Mohammad Sarhan ( mohammad@ ) 
 * @date 7/9/16
 */
class IntegrationSpec extends Specification {

    @TempDirectory(clean = false)
    protected File projectDir

    static String lineEnd = System.getProperty('line.separator')

    protected File settingsFile
    protected File buildFile

    def setup() {

        def moduleName = findModuleName()
        if (!settingsFile) {
            settingsFile = new File(projectDir, 'settings.gradle')
            settingsFile.text = "rootProject.name='${moduleName}'\n"
        }

        if (!buildFile) {
            buildFile = new File(projectDir, 'build.gradle')
        }

        println "Running test from ${projectDir}"

        buildFile << "// Running test for ${moduleName}\n"

    }

    /**
     * Runs the build, asserting that it passes
     * @param isPlugin
     * @param tasks
     * @return
     */
    protected BuildResult runSuccessfully(boolean isPlugin = true, String... tasks) {
        getRunner(isPlugin, tasks).build()
    }

    /**
     * Runs the build, asserting that it fails
     * @param isPlugin
     * @param tasks
     * @return
     */
    protected BuildResult runFailure(boolean isPlugin = true, String... tasks) {
        getRunner(isPlugin, tasks).buildAndFail()
    }


    protected GradleRunner getRunner(boolean isPlugin, String... tasks) {

        def runner = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(tasks)

        if (isPlugin)
            runner.withPluginClasspath()

        return runner
    }

    /**
     * Returns the module name, which is usually the name of the test method.
     * @return
     */
    protected String findModuleName() {
        projectDir.getName().replaceAll(/_\d+/, '')
    }

    /**
     *
     * @param path
     * @return a File with path relative to the project dir
     */
    protected File file(String path) {
        new File(projectDir, path)
    }

    protected File addSubproject(String name) {
        settingsFile << "include '${name}'${lineEnd}"
        def dir = new File(projectDir, name)
        dir.mkdirs()

        dir
    }

    protected File addSubproject(String name, String gradleContents) {
        def dir = addSubproject(name)
        new File(dir, 'build.gradle').text = gradleContents

        dir
    }
}
