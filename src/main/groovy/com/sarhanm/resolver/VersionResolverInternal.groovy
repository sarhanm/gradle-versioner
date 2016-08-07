package com.sarhanm.resolver

import groovy.transform.Memoized
import groovy.transform.Synchronized
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository
import org.gradle.api.logging.Logging
import org.gradle.internal.Transformers
import org.gradle.internal.resolve.result.DefaultBuildableModuleVersionListingResolveResult
import org.gradle.util.CollectionUtils
import org.yaml.snakeyaml.Yaml

/**
 *
 * @author mohammad sarhan
 */
class VersionResolverInternal implements Action<DependencyResolveDetails> {

    def logger = Logging.getLogger(VersionResolverInternal)

    private Project project
    private VersionManifestOption options
    private List<ResolutionAwareRepository> repositories = null
    private File manifestFile
    private usedVersions
    private computedManifest
    private siblingProject = new HashSet();
    private Map<String, String> explicitVersion = [:]

    VersionResolverInternal(Project project, VersionManifestOption manifestOptions = null, File manifestFile = null) {
        this.project = project
        this.repositories = null;
        this.manifestFile = manifestFile
        this.options = manifestOptions

        this.usedVersions = [:]
        this.computedManifest = ['modules': usedVersions]

        project?.getParent()?.subprojects?.each { siblingProject.add("${it.group}:${it.name}"); }


        project?.configurations?.all { Configuration c ->
            c.dependencies.all { Dependency d ->
                if (d.version != 'auto') {
                    String key = "${d.group}:${d.name}"
                    explicitVersion[key] = d.version
                    logger.debug "Adding {} to explicit versions", key
                }
            }
        }

    }

    @Synchronized
    List<ResolutionAwareRepository> getRepos() {
        if (repositories == null) {
            this.repositories = CollectionUtils.collect(project.repositories, Transformers.cast(ResolutionAwareRepository.class))
        }

        return this.repositories
    }

    @Override
    void execute(DependencyResolveDetails details) {

        def requested = details.requested
        def requestedVersion = resolveVersionFromManifest(details)
        def range = new VersionRange(requestedVersion)

        if (range.valid) {
            requestedVersion = resolveVersion(requested.group, requested.name, requestedVersion, range)
        } else {
            logger.debug("$requested is not a valid range ($requestedVersion). Not trying to resolve")
        }

        if (requestedVersion != requested.version) {
            details.useVersion requestedVersion
            usedVersions["${details.requested.group}:${details.requested.name}"] = requestedVersion
            logger.debug("Resolved version of ${details.requested.group}:${details.requested.name} to $requestedVersion")
        }

    }

    def resolveVersion(String group, String name, String version, VersionRange range) {
        logger.debug "$group:$name:$version is a valid range. Trying to resolve"
        def versionToReturn = version
        getRepos().each { repo ->
            if (versionToReturn != version)
                return

            def resolver = repo.createResolver()

            //We don't want to include the local repo if we are trying to re-fresh dependencies
            if (this.project.gradle.startParameter.refreshDependencies && resolver.local) {
                return
            }

            def data
            try {
                // Gradle 3.0+ changed the name of DefaultDependencyMetaData (the casing of "data").
                // Loading the class dynamically depending on the version of gradle.
                Class clazz
                if (project.getGradle().getGradleVersion().startsWith("2"))
                    clazz = this.class.classLoader.loadClass("org.gradle.internal.component.model.DefaultDependencyMetaData")
                else
                    clazz = this.class.classLoader.loadClass("org.gradle.internal.component.model.DefaultDependencyMetadata")

                def versionIdentifier = new DefaultModuleVersionIdentifier(group, name, version)
                data = clazz.newInstance([versionIdentifier] as Object[])
            } catch (Exception e) {
                logger.error "Was not able to create new instance. Not resolving version $version", e
                return
            }

            def result = new DefaultBuildableModuleVersionListingResolveResult()

            resolver.remoteAccess.listModuleVersions(data, result)

            if (result.hasResult()) {
                def latestVersion = null
                result.versions.each {

                    //NOTE: we could use the sort and other cool grooyish methods,
                    // but this should be a lot more efficient as a single pass
                    if (range.contains(it)) {
                        def current = new Version(it)
                        if (latestVersion == null)
                            latestVersion = current
                        else if (latestVersion.compareTo(current) < 0)
                            latestVersion = current
                    }
                }

                if (latestVersion) {
                    logger.info "$group:$name using version $latestVersion.version"
                    versionToReturn = latestVersion.version
                }
            }
        }

        return versionToReturn
    }

    def String resolveVersionFromManifest(DependencyResolveDetails details) {
        def requested = details.requested
        def ver = requested.version
        def group = requested.group
        def name = requested.name

        // Siblings are always versioned together.
        if (siblingProject.contains("${group}:${name}"))
            return ver

        //it actually doesn't matter if the version is 'auto' or otherwise
        // as long as its a version found in our version manifest
        //we always pin the version based on two rules in the following order:
        // 1. if pinned in the build.gradle, then that version wins
        // 2. if pinned in the version manifest, then that version wins.

        //Only do our logic of overriding pins if we are defining them in the version manifest
        //otherwise we should depend on gradle to resolve multiple versions for the same dependency.
        if (isVersionInManifest(group, name)) {
            if (isExplicitlyVersioned(group, name)) {
                //use the version in the build.gradle
                return getExplicitVersionFromProject(group, name)
            } else {
                //resolve via the manifest. We don't care that auto is being used or not,
                //we always resolve to what is in the manifest
                ver = getVersionFromManifest(group, name)
                if (!ver)
                    throw new RuntimeException("Could not resolve manifest location ($manifestFile , $options.url)")

            }
        }

        if (ver == "auto")
            throw new IllegalStateException("[com.sarhanm.vesion-resolver]$group:$name:$ver is " +
                    "marked for resolution but no version is defined in version manifest")

        //return the default version if not resolved by above
        return ver
    }

    @Memoized
    private String getVersionFromManifest(group, name) {
        def manifest = getManifest()
        def key = "${group}:${name}"

        if (!manifest)
            return null

        return manifest.modules[key] ?: null
    }

    @Memoized
    private boolean isVersionInManifest(group, name) {
        return getVersionFromManifest(group, name) != null
    }

    private boolean isExplicitlyVersioned(String group, String name) {
        return getExplicitVersionFromProject(group, name) != null
    }

    private String getExplicitVersionFromProject(String group, String name) {
        return explicitVersion.get("$group:$name" as String)
    }

    /**
     * Defer resolving the configuration until as late as we can.
     * @return
     */
    @Memoized
    @Synchronized
    private File resolveManifestConfiguration() {

        if (manifestFile == null) {
            def versionManifest = project?.configurations?.findByName(VersionResolutionPlugin.VERSION_MANIFEST_CONFIGURATION)

            def resolved = versionManifest?.resolve()
            if (resolved && !resolved.empty) {
                manifestFile = resolved.first()
            }
        }
        manifestFile
    }

    @Synchronized
    @Memoized
    private getManifest() {
        if (manifestFile == null)
            resolveManifestConfiguration()

        def location = manifestFile?.toURI()?.toString() ?: options.url

        //It is possible to use the plugin without providing a manifest.
        // For example, if you just wanted to use version ranges.
        if (!location)
            return null

        def text = null
        if (location.startsWith("http")) {

            def builder = new HTTPBuilder(location)

            if (options.username != null) {
                builder.auth.basic options.username, options.password
            }

            if (options.ignoreSSL)
                builder.ignoreSSLIssues()

            builder.request(Method.GET) { rep ->
                // executed for all successful responses:
                response.success = { resp, reader ->
                    assert resp.statusLine.statusCode == 200
                    text = reader.text
                    logger.debug("Version Manifest: $location")
                }

                response.failure = { resp ->
                    logger.error "Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}"
                }
            }
        } else {
            def url = location.toURL()
            logger.info("Version Manifest: $url")
            text = url.text
        }

        if (text == null)
            return null

        logger.debug(text)

        Yaml yaml = new Yaml()
        def manifest = yaml.load(text)
        manifest
    }

    def getComputedVersionManifest() {
        return computedManifest
    }

}

