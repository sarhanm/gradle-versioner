package com.sarhanm.versioner

/**
 * Seperate class so we can mock and test
 * @author mohammad sarhan
 */
class GitExecutor
{
    def File rootDir
    GitExecutor(def File rootDir) {
        this.rootDir = rootDir
    }

/**
     * Executes the git command
     * @param cmdArgs
     * @return
     */
    def execute(cmdArgs)
    {
        def cmd = "git " + cmdArgs
        def proc = cmd.execute(null, rootDir)

        proc.waitFor()

        if( proc.exitValue() != 0 )
            return null;

        def output = proc.in.text

        return output
    }
}
