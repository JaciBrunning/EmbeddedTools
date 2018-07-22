package jaci.gradle.files

import groovy.transform.CompileStatic

@CompileStatic
interface IDirectoryTree {

    Set<File> getDirectories()
    IDirectoryTree plus(IDirectoryTree other)

}