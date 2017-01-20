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
    void testBranchCleansing() {
        def gitMock = new MockFor(GitExecutor.class)
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }
        gitMock.demand.execute { params -> "origin/master" }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "master", versioner.getCleansedBranchName()
        }

        gitMock.demand.execute { params -> "remote/origin/master" }
        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "master", versioner.getCleansedBranchName()
        }


        gitMock.demand.execute { params -> "remote/origin/feature/my-branch" }
        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "feature-my-branch", versioner.getCleansedBranchName()
        }

        gitMock.demand.execute { params -> "remote/origin/feature/my_branch" }
        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "feature-my_branch", versioner.getCleansedBranchName()
        }

        gitMock.demand.execute { params -> "feature/my.branch" }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "feature-my-branch", versioner.getCleansedBranchName()
        }
    }

    @Test
    void testGitCache() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)
        gitMock.demand.execute(1) { params -> "foobar" }

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
    void testSolidVersionCheck() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute { params -> "origin/master" }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertTrue versioner.useSolidMajorMinorVersion()
        }

        gitMock.demand.execute { params -> "feature/foobar" }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertFalse versioner.useSolidMajorMinorVersion()
        }

        gitMock.demand.execute { params -> "hotfix/foobar" }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertTrue versioner.useSolidMajorMinorVersion()
        }

        gitMock.demand.execute { params -> "release/foobar" }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertTrue versioner.useSolidMajorMinorVersion()
        }
    }

    @Test
    void testHotfixVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

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
            assertEquals "2.3.3.4.hotfix-foobar.adbcdf", versioner.getVersion()
            assertEquals "2.3", versioner.getMajorMinor()
            assertEquals "3.4", versioner.getVersionPoint()
            assertEquals "hotfix-foobar", versioner.getCleansedBranchName()
            assertEquals "adbcdf", versioner.getCommitHash()

            assertEquals 2, versioner.getMajorNumber()
            assertEquals 3, versioner.getMinorNumber()
            assertEquals 3, versioner.getPointNumber()
            assertEquals 4, versioner.getHotfixNumber()
        }
    }

    @Test
    void testReleaseHotfixVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(6) { params ->
            if (params == Versioner.CMD_BRANCH) "release/hotfix"
            else if (params.startsWith("log --oneline release/hotfix")) "one\ntwo\nthree\nfour"
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
            assertEquals "2.3.3.4.release-hotfix.adbcdf", versioner.getVersion()
            assertEquals 4, versioner.getHotfixNumber()
        }
    }


    @Test
    void testHotfixBranchWithHotfixDisabledVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(4) { params ->
            if (params == Versioner.CMD_BRANCH) "hotfix/foobar"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v3.9"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdff"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v3.9-5-gcd4c01a"
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
    void testFeatureVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

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
    void testVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(4) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v3.9"
            else if (params == Versioner.CMD_POINT) "1005"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdff"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v3.9-1005-gcd4c01a"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "3.9.1005.master.adbcdff", versioner.getVersion()
        }
    }

    @Test
    void testToStringVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(4) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v3.9"
            else if (params == Versioner.CMD_POINT) "1005"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdff"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v3.9-1005-gcd4c01a"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "3.9.1005.master.adbcdff", versioner.toString()
        }
    }

    @Test
    void testToStringVersionWithDisabledVersioner() {
        def versioner = new Versioner()
        // Test the disabled behavior, normally should return the
        // initial project version but the test constructor uses 1.0
        versioner.options.disabled = true
        assertEquals "1.0", versioner.toString()
    }

    @Test
    void testPlusVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(4) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v3.9"
            else if (params == Versioner.CMD_POINT) "1005"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdff"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v3.9-1005-gcd4c01a"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "3.9.1005.master.adbcdff-hi", versioner + "-hi"
        }
    }

    @Test
    void testSnapshotVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

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
    void testSnapshotSolidVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

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
    void testSolidDigitOnlyVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdf"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v2.3-4-gcd4c01a"
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
    void testSolidDigitOnlyOnSolidBranchVersion() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdf"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v2.3-4-gcd4c01a"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            versioner.options.omitBranchMetadataOnSolidBranch = true
            assertEquals "2.3.4", versioner.getVersion()
        }
    }

    @Test
    public void testNullPointDescribeTag() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdf"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) null
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "2.3.4.master.adbcdf", versioner.getVersion()
        }
    }


    @Test
    public void testIncompletePointDescribeTag() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdf"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v2.3"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "2.3.4.master.adbcdf", versioner.getVersion()
        }
    }

    @Test
    public void testIncompletePointDescribeTag2() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdf"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v2.3-"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "2.3.4.master.adbcdf", versioner.getVersion()
        }
    }

    @Test
    public void testEmptyPointDescribeTag() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..5) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdf"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) ""
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            assertEquals "2.3.4.master.adbcdf", versioner.getVersion()
        }
    }

    @Test
    public void testGitData() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(1..6) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v2.3"
            else if (params == Versioner.CMD_POINT) "4"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdf"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) ""
            else if (params.startsWith("log --oneline ")) "one\ntwo\nthree\nfour"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            GitData gitData = new GitData(versioner)

            assertEquals gitData.version, versioner.getVersion()
            assertEquals gitData.major, versioner.getMajorNumber()
            assertEquals gitData.minor, versioner.getMinorNumber()
            assertEquals gitData.point, versioner.getPointNumber()
            assertEquals gitData.hotfix, versioner.getHotfixNumber()
            assertEquals gitData.branch, versioner.branch
            assertEquals gitData.commit, versioner.commitHash
            assertEquals gitData.totalCommits, Integer.parseInt(versioner.getTotalCommits())
        }
    }

    @Test
    void testVersionWithBranchDisplayOverride() {
        def envMock = new MockFor(EnvReader.class)

        envMock.demand.getBranchNameFromEnv(1) { params -> null }

        def gitMock = new MockFor(GitExecutor.class)

        gitMock.demand.execute(4) { params ->
            if (params == Versioner.CMD_BRANCH) "master"
            else if (params == Versioner.CMD_MAJOR_MINOR) "v3.9"
            else if (params == Versioner.CMD_POINT) "1005"
            else if (params == Versioner.CMD_COMMIT_HASH) "adbcdff"
            else if (params == Versioner.CMD_POINT_SOLID_BRANCH) "v3.9-1005-gcd4c01a"
            else throw new Exception("Unaccounted for method call")
        }

        gitMock.use {
            def versioner = new Versioner()
            versioner.envReader = envMock.proxyInstance()
            versioner.options.branchNameDisplayOverride = {v -> "override"}
            assertEquals "3.9.1005.override.adbcdff", versioner.getVersion()
        }
    }
}
