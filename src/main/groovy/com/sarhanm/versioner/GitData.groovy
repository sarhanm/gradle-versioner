package com.sarhanm.versioner

/**
 * Git version data returned from the Versioner plugin for use in the build script.
 *
 * @author mohammad sarhan
 */
class GitData {
    def major;      // Major version number (n.0.0)
    def minor;      // Minor version number (0.n.0)
    def point;      // Point version number (0.0.n)
    def hotfix;     // Optional hotfix version number (0.0.0.n)
    def branch;     // Git branch name ("master")
    def commit;     // Git short commit hash ("5f817fb142")
}
