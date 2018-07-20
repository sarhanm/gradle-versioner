package com.sarhanm.resolver

import groovy.transform.Memoized
import groovy.transform.Synchronized
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.Transformers
import org.gradle.util.CollectionUtils
import org.yaml.snakeyaml.Yaml

/**
 *
 * @author mohammad sarhan
 */
class VersionResolverInternal implements Action<DependencyResolveDetails> {

    private static final Logger logger = Logging.getLogger(VersionResolverInternal)

    private Project project
    private ConfigurationContainer configurations
    private VersionManifestOption options
    private RepositoryHandler repositoryHandler

    private List<ResolutionAwareRepository> repositories = null
    private usedVersions
    private computedManifest
    private Map<String, String> explicitVersion = [:]

    VersionResolverInternal(Project project,
                            VersionManifestOption manifestOptions,
                            ConfigurationContainer configurations,
                            RepositoryHandler repositoryHandler
    ) {
        this(project, manifestOptions, configurations, repositoryHandler, null);
    }


    VersionResolverInternal(Project project,
                            VersionManifestOption manifestOptions,
                            ConfigurationContainer configurations,
                            RepositoryHandler repositoryHandler,
                            Set<Project> subprojects
    ) {

        this.project = project
        this.configurations = configurations
        this.repositoryHandler = repositoryHandler
        this.options = manifestOptions

        this.usedVersions = [:]
        this.computedManifest = ['modules': usedVersions]

        this.configurations?.all { Configuration c ->
            c.dependencies.all { Dependency d ->
                if (d.version != 'auto') {
                    String key = "${d.group}:${d.name}"
                    explicitVersion[key] = d.version
                    logger.quiet "Adding {} to explicit versions with version {}", key, d.version
                }
            }
        }

    }

    @Synchronized
    List<ResolutionAwareRepository> getRepos() {
        if (repositories == null) {
            this.repositories = CollectionUtils.collect(repositoryHandler, Transformers.cast(ResolutionAwareRepository.class))
        }

        return this.repositories
    }

    @Override
    void execute(DependencyResolveDetails details) {

        def requested = details.requested
        def requestedVersion = resolveVersion(requested.group, requested.name, requested.version)
        def range = new VersionRange(requestedVersion)

        if (range.valid) {
            //replace with a range query so the componentselection logic kicks in.
            //TODO: restrict range to so we don't have to validate every possible version.
            requestedVersion = '+'
        } else {
            logger.debug("$requested is not a valid range ($requestedVersion). Not trying to resolve")
        }

        if (requestedVersion != requested.version) {
            details.useVersion requestedVersion
            usedVersions["${details.requested.group}:${details.requested.name}"] = requestedVersion
            logger.debug("Resolved version of ${details.requested.group}:${details.requested.name} to $requestedVersion")
        }
    }

    String resolveVersion(String group, String name, String requestedVersion) {

        // Local projects are always versioned together.
        if (isLocalProject(group, name)) {
            logger.quiet("$group:$name is a sibling project. skipping version resolution")
            return requestedVersion
        }

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
                requestedVersion = getVersionFromManifest(group, name)
                if (!requestedVersion)
                    throw new RuntimeException("Could not resolve manifest location (${resolveManifestConfiguration()} , ${options?.url})")

            }
        }

        if (requestedVersion == "auto")
            throw new IllegalStateException("[com.sarhanm.vesion-resolver]$group:$name:$requestedVersion is " +
                    "marked for resolution but no version is defined in version manifest")

        //return the default version if not resolved by above
        return requestedVersion
    }

    @Memoized
    String getVersionFromManifest(group, name) {
        def manifest = getManifest()
        def key = "${group}:${name}"

        if (!manifest)
            return null

        return manifest.modules[key] ?: null
    }

    @Memoized
    boolean isVersionInManifest(group, name) {
        return getVersionFromManifest(group, name) != null
    }

    boolean isExplicitlyVersioned(String group, String name) {
        return getExplicitVersionFromProject(group, name) != null
    }

    String getExplicitVersionFromProject(String group, String name) {
        return explicitVersion.get("$group:$name" as String)
    }

    /**
     * Defer resolving the configuration until as late as we can.
     * @return
     */
    @Memoized
    @Synchronized
    private List<File> resolveManifestConfiguration() {
        def versionManifest = configurations?.findByName(VersionResolutionPlugin.VERSION_MANIFEST_CONFIGURATION)
        def resolved = versionManifest?.resolve()
        resolved && !resolved.empty ? new ArrayList<>(resolved) : null
    }

    @Synchronized
    @Memoized
    private getManifest() {

        def filesToLoad = []

        resolveManifestConfiguration()?.each {
            filesToLoad.add it.toURI()
        }

        if (options?.url) {
            logger.info("Resolving ${options.url}")

            if (options.url.startsWith("http")) {
                filesToLoad.add options.url.toURI()
            } else {
                def manifestFile = project.file(options.url)
                if (!manifestFile.exists()) {
                    logger.info("Not loading ${options.url} because it does not exist")
                } else {
                    logger.info("Loading ${manifestFile.toURI()}")
                    filesToLoad.add manifestFile.toURI()
                }
            }
        }

        //It is possible to use the plugin without providing a manifest.
        // For example, if you just wanted to use version ranges.
        if (filesToLoad.isEmpty())
            return


        def allModules = [:]

        Yaml yamlLoader = new Yaml()

        filesToLoad.each { URI location ->

            def text = null

            if (location.scheme.startsWith("http")) {

                def builder = new HTTPBuilder(location)

                if (options?.username != null) {
                    builder.auth.basic options.username, options.password
                }

                if (options?.ignoreSSL)
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

            if (!text)
                return null


            logger.debug("Loaded version manifest $location\n$text")

            //Load manifest and append to existing set of modules
            Map yaml = yamlLoader.load(text)
            if (yaml.containsKey("modules")) {
                Map modules = yaml['modules']
                if (modules) {
                    allModules << modules
                }
            }
        }

        return [modules: allModules]
    }

    def getComputedVersionManifest() {
        return computedManifest
    }

    boolean isLocalProject(group, name) {
        project.getRootProject().getAllprojects().find { p -> p.group == group && p.name == name }
    }

}

