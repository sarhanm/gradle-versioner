package com.sarhan.versioner

/**
 * Mostly to be mockable
 * @author mohammad sarhan
 */
class EnvReader
{

    def getBranchNameFromEnv()
    {
        //Travis branch name
        def name = System.env.TRAVIS_BRANCH

        //Jenkins branch name
        if(!name)
            name = System.env.GIT_BRANCH

        return name
    }
}
