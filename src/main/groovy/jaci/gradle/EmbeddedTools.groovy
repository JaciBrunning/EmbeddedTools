package jaci.gradle

import org.gradle.api.*;
import groovy.util.*;

import org.gradle.model.*;

class DeployTools implements Plugin<Project> {
    void apply(Project project) {
        
    }

    static class DeployRules extends RuleSource {
        @Model("targets")
        void createTargetsModel(TargetsSpec spec) { }
    }
}