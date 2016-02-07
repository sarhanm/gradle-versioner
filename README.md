# [[Gradle Versioner Plugin|Home]

[![Build Status](https://travis-ci.org/sarhanm/gradle-versioner.svg?branch=master)](https://travis-ci.org/sarhanm/gradle-versioner)

## Overview

With this plugin, you can create project versions like: 

```2.1.6.master.d9741b1``` or ```0.0.6.feature-fixing-something.c3751f4```


The values in the version string are derived from the state of the git repository. This allows you to:

1. Not worry about what the project version should be
1. Project version is set based on your branch name so
    1. Alleviates issues of updating the version before you push.
    1. You know where a version of a library came from
1. The commit hash is part of the version string so hot fixing is based off the correct commit hash, not from a branch you think is probably the right place to fork.

## Documentation

[[Full Documenation is on the wiki|Home]] 