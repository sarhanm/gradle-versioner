package com.sarhanm.resolver

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

/**
 * Task to output the computed version manifest to a well-known location
 * @author mohammad sarhan
 */
class VersionManifestOutputTask extends DefaultTask {

    @Input
    def VersionResolver versionResolver

    @OutputFile
    def File outputFile

    @TaskAction
    def outputManifest() {
        def data = versionResolver.getComputedVersionManifest()

        def options = new DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.prettyFlow = true
        options.defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN

        def yaml = new Yaml(options).dump(data)

        def w = outputFile.newWriter()
        w << yaml
        w.close()
    }
}
