package com.sarhan.versioner

/**
 * Seperate class so we can mock and test
 * @author mohammad sarhan
 */
class GitExecutor
{
    /**
     * Executes the git command
     * @param cmdArgs
     * @return
     */
    def execute(cmdArgs)
    {
        def cmd = "git " + cmdArgs
        def proc = cmd.execute()

        proc.waitFor()

        if( proc.exitValue() != 0 )
            return null;

        def output = proc.in.text

        return output
    }
}
