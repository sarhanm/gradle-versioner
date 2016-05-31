package com.sarhanm.versioner

/**
 * Mostly to be mockable
 * @author mohammad sarhan
 */
class EnvReader
{
    def getBranchNameFromEnv(branchNameEnv=null)
    {
        if (branchNameEnv && System.env[branchNameEnv] != null) {
            return System.env[branchNameEnv]
        }

        //Travis branch name
        //Jenkins branch name
        //Jenkins pipeline branch name
        for (env in ['TRAVIS_BRANCH', 'GIT_BRANCH', 'BRANCH_NAME']) {
            if (System.env[env] != null) {
                return System.env[env]
            }
        }
    }
}
