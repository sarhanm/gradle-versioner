package com.sarhanm.versioner

import groovy.mock.interceptor.MockFor
import org.junit.Test

import static org.junit.Assert.*

/**
 *
 * @author mohammad sarhan
 */
class VersionerTest {


    @Test
    void testBranchCleansing()
    {
        def gitMock = new MockFor(GitExecutor.class)
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv { params -> null }
        gitMock.demand.execute {params -> "origin/master"}

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "master" , versioner.getCleansedBranchName()
        }

        gitMock.demand.execute {params -> "remote/origin/master"}
        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "master" , versioner.getCleansedBranchName()
        }


        gitMock.demand.execute {params -> "remote/origin/feature/my-branch"}
        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "feature-my-branch" , versioner.getCleansedBranchName()
        }

        gitMock.demand.execute {params -> "remote/origin/feature/my_branch"}
        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "feature-my_branch" , versioner.getCleansedBranchName()
        }

        gitMock.demand.execute {params -> "feature/my.branch"}

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "feature-my-branch" , versioner.getCleansedBranchName()
        }
    }

    @Test
    void testGitCache()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)
        gitMock.demand.execute(1){ params -> "foobar"}

        gitMock.use {
            //Ensure that we are caching properly
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            def result = versioner.getCleansedBranchName()
            def result2 = versioner.getCleansedBranchName()
            assertEquals result, result2
        }
    }

    @Test
    void testSolidVersionCheck()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute {params -> "origin/master"}

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertTrue versioner.useSolidMajorMinorVersion()
        }

        gitMock.demand.execute {params -> "feature/foobar"}

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertFalse versioner.useSolidMajorMinorVersion()
        }

        gitMock.demand.execute {params -> "hotfix/foobar"}

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertTrue versioner.useSolidMajorMinorVersion()
        }

        gitMock.demand.execute {params -> "release/foobar"}

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertTrue versioner.useSolidMajorMinorVersion()
        }
    }

    @Test
    void testHotfixVersion()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(6) { params ->
            if (params == Versioner.CMD_BRANCH) "hotfix/foobar"
            else if (params.startsWith("log --oneline hotfix/foobar")) "one\ntwo\nthree\nfour"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdf"
            else if (params.startsWith("merge-base")) "commit-hash-return"
            else if (params == "log --oneline commit-hash-return") "one\ntwo\nthree"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "hotfix-foobar" , versioner.getCleansedBranchName()
            assertEquals "2.3.3.4.hotfix-foobar.adbcdf", versioner.getVersion()
        }
    }

    @Test
    void testHotfixBranchWithHotfixDisabledVersion()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(4) { params ->
            if (params == Versioner.CMD_BRANCH) "hotfix/foobar"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v3.9"
            else if (params == Versioner.CMD_POINT) "5"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdff"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            versioner.options.disableHotfixVersioning = true
            assertEquals "3.9.5.hotfix-foobar.adbcdff", versioner.getVersion()
        }
    }

    @Test
    void testFeatureVersion()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(3) { params ->
            if (params == Versioner.CMD_BRANCH) "feature/foobar"
            else if (params == Versioner.CMD_POINT) "9"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdf"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            versioner.options.omitBranchMetadataOnSolidBranch = true
            assertEquals "0.0.9.feature-foobar.adbcdf", versioner.getVersion()
        }
    }

    @Test
    void testVersion()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

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
            versioner.envReader = envMock.proxyInstance()
            assertEquals "3.9.1005.master.adbcdff", versioner.getVersion()
        }
    }

    @Test
    void testSnapshotVersion()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdf"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            versioner.options.snapshot = true
            assertEquals "2.3.master-SNAPSHOT", versioner.getVersion()
        }
    }

    @Test
    void testSnapshotSolidVersion()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdf"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            versioner.options.snapshot = true
            versioner.options.omitBranchMetadata = true
            assertEquals "2.3-SNAPSHOT", versioner.getVersion()
        }
    }

    @Test
    void testSolidDigitOnlyVersion()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdf"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            versioner.options.omitBranchMetadata = true
            assertEquals "2.3.4", versioner.getVersion()
        }
    }

    @Test
    void testSolidDigitOnlyOnSolidBranchVersion()
    {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1..10) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.getCMD_COMMIT_HASH()) "adbcdf"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            versioner.options.omitBranchMetadataOnSolidBranch = true
            assertEquals "2.3.4", versioner.getVersion()
        }
    }




}
