package com.sarhanm.versioner

import org.gradle.api.Project

/**
 * Separate class so we can mock and test
 * @author mohammad sarhan
 */
class GitExecutor
{
    def Project project
    GitExecutor(def Project project) {
        this.project = project
    }

    /**
     * Executes the git command
     * @param cmdArgs
     * @return output from command execution
     */
    def execute(cmdArgs)
    {
        def cmd = "git " + cmdArgs

        new ByteArrayOutputStream().withStream { output ->
            new ByteArrayOutputStream().withStream { error ->

                def result = project.exec {
                    commandLine = cmd.split(' ') as List
                    workingDir = project.projectDir
                    ignoreExitValue = true
                    standardOutput = output
                    errorOutput = error
                }

                if ( result.exitValue == 0 )
                    return output.toString()

                return null
            }
        }
    }
}
