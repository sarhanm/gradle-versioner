package com.sarhanm.resolver

/**
 *
 * @author mohammad sarhan
 */
class VersionResolverOptions {

    def boolean outputComputedManifest = false

    /**
     * If true (default) it will include solid versions in the generated pom file.
     *
     * By default, gradle will use whatever version is specified by the user (in our case, the word "auto")
     * when generating the pom. This option will  always use the resolved version of the module in the pom file.
     *
     */
    def boolean resolveGeneratedPomVersions = true
}

class VersionManifestOption
{
    /**
     * The url of the version manifest. Can be a local file location as well as a remote (https, http) location
     */
    def String url

    /**
     *  Only if the file requires a username and password to access via http[s].
     *  The username and password are used via basic auth.
     */
    def String username
    def String password

    /**
     * Set to true if the URL is located at a domain with a self-signed cert. This
     * will ignore any ssl errors.
     */
    def boolean ignoreSSL
}

