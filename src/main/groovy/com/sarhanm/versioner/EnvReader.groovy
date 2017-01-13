package com.sarhanm.versioner

/**
 * Mostly to be mockable
 * @author mohammad sarhan
 */
class EnvReader {
    def getEnvValue(env) {
        return System.env[env]
    }

    def getBranchNameFromEnv(branchNameEnv = null) {
        if (branchNameEnv && getEnvValue(branchNameEnv) != null) {
            return getEnvValue(branchNameEnv)
        }

        //Travis branch name
        //Jenkins branch name
        //Jenkins pipeline branch name
        for (env in ['TRAVIS_BRANCH', 'GIT_BRANCH', 'BRANCH_NAME']) {
            if (getEnvValue(env) != null) {
                return getEnvValue(env)
            }
        }
    }
}
