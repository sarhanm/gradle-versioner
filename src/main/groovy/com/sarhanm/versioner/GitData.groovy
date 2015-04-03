package com.sarhanm.versioner

/**
 * Git version data returned from the Versioner plugin for use in the build script.
 *
 * @author mohammad sarhan
 */
class GitData {
    int major;      // Major version number (n.0.0)
    int minor;      // Minor version number (0.n.0)
    int point;      // Point version number (0.0.n)
    int hotfix;     // Optional hotfix version number (0.0.0.n)
    String branch;     // Git branch name ("master")
    String commit;     // Git short commit hash ("5f817fb142")
}
