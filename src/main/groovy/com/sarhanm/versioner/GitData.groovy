package com.sarhanm.versioner

/**
 * Git version data returned from the Versioner plugin for use in the build script.
 *
 * @author mohammad sarhan
 */
class GitData {
    private Versioner versioner

    GitData(Versioner versioner) {
        this.versioner = versioner
    }

    // The computed version, may be different from project version
    // if Versioner is disabled.
    String getVersion() {
        versioner.getVersion()
    }

    // Major version number (n.0.0)
    int getMajor() {
        versioner.getMajorNumber()
    }

    // Minor version number (0.n.0)
    int getMinor() {
        versioner.getMinorNumber()
    }

    // Point version number (0.0.n)
    int getPoint() {
        versioner.getPointNumber()
    }

    // Optional hotfix version number (0.0.0.n)
    int getHotfix() {
        versioner.getHotfixNumber()
    }

    // Git branch name ("master")
    String getBranch() {
        versioner.branch
    }

    // Git short commit hash ("5f817fb142")
    String getCommit() {
        versioner.commitHash
    }

    // Total number of commits.
    int getTotalCommits() {
        Integer.parseInt(versioner.getTotalCommits())
    }
}
