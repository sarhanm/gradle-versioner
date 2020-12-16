# [Gradle Versioner Plugin](https://github.com/sarhanm/gradle-versioner/wiki)

[![Build Status](https://travis-ci.org/sarhanm/gradle-versioner.svg?branch=master)](https://travis-ci.org/sarhanm/gradle-versioner)
 [![Coverage Status](https://coveralls.io/repos/github/sarhanm/gradle-versioner/badge.svg?branch=master)](https://coveralls.io/github/sarhanm/gradle-versioner?branch=master)
## Overview

With this plugin, you can create project versions like: 

```2.1.6.master.d9741b1``` or ```0.0.6.feature-fixing-something.c3751f4```


The values in the version string are derived from the state of the git repository. This allows you to:

1. Not worry about what the project version should be
1. Project version is set based on your branch name so
    1. Alleviates issues of updating the version before you push.
    1. You know where a version of a library came from
1. The commit hash is part of the version string so hot fixing is based off the correct commit hash, not from a branch you think is probably the right place to fork.

## Adding to your project

Outlined here: https://plugins.gradle.org/plugin/com.sarhanm.versioner

Buildscript DSL
```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.sarhanm:gradle-versioner:4.0.0"
  }
}

// The versioner
apply plugin: "com.sarhanm.versioner"

// Optional: the version resolver
apply plugin: "com.sarhanm.version-resolver"
```

Using the plugins mechanism
```
plugins {
  //Versioner
  id "com.sarhanm.versioner" version "4.0.0"
  
  //Version Resolver (optional)
  id "com.sarhanm.version-resolver" version "4.0.0"
}
```

## Full Documentation

[Full Documenation is on the wiki](https://github.com/sarhanm/gradle-versioner/wiki) 