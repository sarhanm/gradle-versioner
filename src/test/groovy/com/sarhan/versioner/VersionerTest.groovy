package com.sarhan.versioner

import groovy.mock.interceptor.MockFor
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

/**
 *
 * @author mohammad sarhan
 */
class VersionerTest {


    @Test
    void testBranchCleansing()
    {
        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute {params -> "origin/master"}

        gitMock.use {
            def versioner = new Versioner()
            assertEquals "master" , versioner.getBranchName()
        }

        gitMock.demand.execute {params -> "remote/origin/master"}
        gitMock.use {
            def versioner = new Versioner()
            assertEquals "master" , versioner.getBranchName()
        }


        gitMock.demand.execute {params -> "remote/origin/feature/my-branch"}
        gitMock.use {
            def versioner = new Versioner()
            assertEquals "feature-my-branch" , versioner.getBranchName()
        }

        gitMock.demand.execute {params -> "remote/origin/feature/my_branch"}
        gitMock.use {
            def versioner = new Versioner()
            assertEquals "feature-my_branch" , versioner.getBranchName()
        }

        gitMock.demand.execute {params -> "feature/my.branch"}

        gitMock.use {
            def versioner = new Versioner()
            assertEquals "feature-my-branch" , versioner.getBranchName()
        }
    }

    @Test
    void testGitCache()
    {
        def gitMock = new MockFor(GitExecutor.class)
        gitMock.demand.execute(1){ params -> "foobar"}

        gitMock.use {
            //Ensure that we are caching properly
            def versioner = new Versioner()
            def result = versioner.getBranchName()
            def result2 = versioner.getBranchName()
            assertEquals result, result2
        }
    }

    @Test
    void testSolidVersionCheck()
    {
        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute {params -> "origin/master"}

        gitMock.use {
            def versioner = new Versioner()
            assertTrue versioner.useSolidVersion()
        }

        gitMock.demand.execute {params -> "feature/foobar"}

        gitMock.use {
            def versioner = new Versioner()
            assertFalse versioner.useSolidVersion()
        }

        gitMock.demand.execute {params -> "hotfix/foobar"}

        gitMock.use {
            def versioner = new Versioner()
            assertTrue versioner.useSolidVersion()
        }

        gitMock.demand.execute {params -> "release/foobar"}

        gitMock.use {
            def versioner = new Versioner()
            assertTrue versioner.useSolidVersion()
        }
    }

    @Test
    void testHotfixVersion()
    {
        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(5) { params ->
            if (params == Versioner.CMD_BRANCH) "hotfix/foobar"
            else if (params.startsWith("log --oneline hotfix/foobar")) "one\ntwo\nthree"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdf"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            assertEquals "hotfix-foobar" , versioner.getBranchName()
            assertEquals "2.3.4.3.hotfix-foobar.adbcdf", versioner.getVersion()

        }
    }

    @Test
    void testFeatureVersion()
    {
        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(3) { params ->
            if (params == Versioner.CMD_BRANCH) "feature/foobar"
            else if (params == Versioner.CMD_POINT) "9"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdf"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            assertEquals "0.0.9.feature-foobar.adbcdf", versioner.getVersion()
        }
    }

    @Test
    void testVersion()
    {
        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(4) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v3.9"
            else if (params == Versioner.CMD_POINT) "1005"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdff"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            assertEquals "3.9.1005.master.adbcdff", versioner.getVersion()
        }
    }
}
