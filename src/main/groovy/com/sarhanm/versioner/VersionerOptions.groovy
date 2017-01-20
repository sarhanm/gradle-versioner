package com.sarhanm.versioner

/**
 * Extension closure to pass parameters from the build script
 * to the plugin
 *
 * Example:
 *
 * build.gradle:
 *     versioner{
 *         snapshot = true
 *     }
 *
 * @author mohammad sarhan
 */
class VersionerOptions {

    /**
     * Set to true to skip setting the version
     */
    def boolean disabled = false

    /**
     * Set to true to generate a snapshot version
     */
    def boolean snapshot = false

    /**
     * Set to true if you want to ommit all branch meta-data and only output digits.
     */
    def boolean omitBranchMetadata = false

    /**
     * Set to true if you want to omit the branch meta-data and only output digits for solidBranches
     */
    def boolean omitBranchMetadataOnSolidBranch = false

    /**
     * This option is available because ios apps uploaded to the apple store require exactly 3 digit version numbers.
     * So having a 4th hotfix value will prevent successful submissions.
     */
    def boolean disableHotfixVersioning = false

    /**
     * Set to the environment Key that contains the branch name.
     * This is ONLY required for build systems other than jenkins and travis
     */
    def String branchEnvName = null

    /**
     * A closure that can return a custom value for the branch name displayed in the version string.  The current Versioner
     * instance will be passed as a parameter to the closure when called.
     */
    def Closure<String> branchNameDisplayOverride = null

    /**
     * The regex used to match branch names which determines which branches get
     * a non-zero major and minor versions
     */
    def String solidBranchRegex = Versioner.DEFAULT_SOLID_BRANCH_REGEX

    /**
     * The branch name of the common hotfix branch. Its the name of the branh
     * that you fork from when you create a hotfix. Currently defaults to 'master'
     */
    def String commonHotfixBranch = Versioner.DEFAULT_HOTFIX_COMMON_BRANCH

}
