package com.sarhanm.versioner

/**
 * Extension closure to pass parameters from the build script
 * to the plugin
 *
 * Example:
 *
 * build.gradle:
 *
 *     versioner{
 *         snapshot = true
 *     }
 *
 *
 * @author mohammad sarhan
 */
class VersionerOptions {
    def boolean disabled = false
    def boolean snapshot = false
    def boolean omitBranchMetadata = false
    def String solidBranchRegex = Versioner.DEFAULT_SOLID_BRANCH_REGEX
    def String commonHotfixBranch = Versioner.DEFAULT_HOTFIX_COMMON_BRANCH

}
