# Gradle Version Resolver

The version resolver plugin allows us to set dynamic versions or version ranges on dependencies that are versioned with the [versioner](versioner.md) plugin

Example:

    # build.gradle
    
    apply plugin: 'com.sarhanm.version-resolver'
    
    
    dependencies{
        compile 'com.sarhanm:my-project:1.2.master+
        compile 'com.sarhanm:my-other-proj:[1.2.3.feature-fixing-something,)
        compile 'com.sarhanm:another-proj:(1.2.3.master,1.3.0.master)
    }
    
Each dynamic version or range is bound to a branch and cannot span branches, since there is no inherit ordering among branches.
 
Version ranges are your typica ranges. '[' and ']' mean inclusive and '(' and ')' mean exclusive. 

Most users will just use the dynamic ranges. 

Here are some examples to help explain how they work

    # build.gradle
        
    apply plugin: 'com.sarhanm.version-resolver'
    
    
    dependencies{
        compile 'com.sarhanm:my-project:1.2.3.master+
        compile 'com.sarhanm:my-project-a:1.2.n.master+
        compile 'com.sarhanm:my-project-b:1.n.n.master+
        compile 'com.sarhanm:my-project-c:1.3.2.n.hotfix-fixing-issue+
        compile 'com.sarhanm:my-project-d:1.3.n.feature-my-fix+        
    }  
    
Dynamic versions require at least the first three digits or 'n' (which means any number) and the branch. 

1.2.n.master+ means any version greater than 1.2 from the master branch.

1.n.n.feature-foobar+ means any version greather than 1 from the feature/foobar branch

You can review the [unit test](../src/test/groovy/com/sarhanm/resolver/VersionDynamicTest.groovy) to get a better idea of what constitutes a valid dynamic version