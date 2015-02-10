package com.sarhanm.resolver

/**
 *
 * @author mohammad sarhan
 */
class VersionResolverOptions {

    /**
     * Path (file://, http://, and https:// are supported)
     * to the full list of versions that should be used in your project.
      */
    def VersionManifestOption manifest = new VersionManifestOption()

    def boolean outputComputedManifest = false
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

