# Gradle Versioner Plugin

[![Build Status](https://travis-ci.org/sarhanm/gradle-versioner.svg?branch=master)](https://travis-ci.org/sarhanm/gradle-versioner)

| Gradle Version | Compatible Versioner Version |
|----------------|:----------------------------:|
| < 2.2.1        | 1.0.58.master.5666bb7        |
| 2.2.1 - 2.3    | 2.0+                         |
| >= 2.4         | 2.1+                         |

 
## The Versioner

For gradle projects, this plugin can generate a version based on git tags and other pieces of information from the git repo. Because the version is derived from the state of your git repo, you don't have to worry about maintaining the version value in your gradle.build file as you merge and/or branch; it'll always output the correct value.

For example, if i had a gradle build that generated a jar, in the master branch the version of the jar would be

    Version: 2.1.6.master.d9741b1
    Produced Artifact: my-project-2.1.6.master.d9741b1.jar
    
and in the feature/fixing-something branch, it would be

    Version: 0.0.6.feature-fixing-something.c3751f4
    Produced artifact: my-project-0.0.6.feature-fixing-something.c3751f4.jar

The version of the project was derived from the state of the git repo. 

We also have the commit-hash at the end, so if we ever wanted to branch off a particular commit, or checkout that commit, the information is stored in the version string.

[You can read more about the gradle-versioner, how its can be used and configured](doc/versioner.md)

## The Version Resolver

The version resolver has two functionalities.

### Resolving version ranges

The version-resolver allows us to specify version ranges and dynamic gradle versions.

So you would be able to do something like

    compile 'com.sarhanm:my-project:1.2.master+
    compile 'com.sarhanm:my-other-proj:[1.2.3.feature-fixing-something,)
    compile 'com.sarhanm:another-proj:(1.2.3.master,1.3.0.master) 

Read the [full documentation on version-resolver](doc/resolver.md)

### Version Manifest Resolution

When using the version-resolver plugin, you can specify a yaml file that contains all the versions of all the dependencies your company will use.

This is similar to maven's "dependencyManagement". 

Read the [full documentation on the version manifest](doc/manifest.md)