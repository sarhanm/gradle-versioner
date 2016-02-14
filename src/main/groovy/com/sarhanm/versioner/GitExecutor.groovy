package com.sarhanm.versioner

import org.apache.tools.ant.types.Commandline
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

        //Deal with parameters that have spaces but are quoted
        // as well as other edge cases
        def commandArray = Commandline.translateCommandline(cmd)

        new ByteArrayOutputStream().withStream { output ->
            new ByteArrayOutputStream().withStream { error ->

                def result = project.exec {
                    commandLine = commandArray
                    workingDir = project.projectDir
                    ignoreExitValue = true
                    standardOutput = output
                    errorOutput = error
                }

                if ( result.exitValue == 0 )
                    return output.toString().trim()
                else{
                    project.logger.debug "ERROR Executing git $cmdArgs: $error"
                }

                return null
            }
        }
    }
}
