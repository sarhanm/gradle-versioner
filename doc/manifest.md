# Version Manifest

### This feature is a work in progress. 

Some companies keep a manifest of all the dependencies in use and the version numbers of those dependencies. This feature is very similar to maven's dependencyManagement.

Simply define a yaml file that contains all the dependencies and their verisons. The yaml file MUST contain

1. A version of the yaml (this is used for backward compatibility)
2. A 'modules' map that define your dependencies

Example:

    # versions.yaml
    version: 1.0
    modules:
      'com.coinfling:auth-service-api': 1.0-SNAPSHOT
      'com.coinfling:cc-service': 1.3.4.master.abfd34

Once you have the file somewhere, you can then use 'auto' as the version of your dependencies in your gradle build.

    # build.gradle
    apply plugin: 'com.sarhanm.version-resolver'
    
    versionResolver{
        versionManifest = 'http://my-url.com/versions.yaml'
    }
    
    dependencies{
        compile 'com.coinfling:auth-service-api:auto'
        compile 'com.coinfling:cc-service:auto'                
    }
    
As always, you can override the version by using something other than 'auto'
