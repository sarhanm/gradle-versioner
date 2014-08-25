package com.sarhanm.versioner

/**
 * Mostly to be mockable
 * @author mohammad sarhan
 */
class EnvReader
{

    def getBranchNameFromEnv(branchNameEnv=null)
    {
        if(branchNameEnv)
        {
            return System.env[branchNameEnv]
        }

        //Travis branch name
        def name = System.env.TRAVIS_BRANCH

        //Jenkins branch name
        if(!name)
            name = System.env.GIT_BRANCH

        return name
    }
}
