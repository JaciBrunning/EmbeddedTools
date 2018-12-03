package jaci.gradle.log

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory

@CompileStatic
class ETLoggerFactory {
    public static ETLoggerFactory INSTANCE = new ETLoggerFactory()

    private StyledTextOutput output = null

    void addColorOutput(Project project) {
        def factory = ((ProjectInternal) project).services.get(StyledTextOutputFactory)
        this.output = factory.create(this.class)
    }

    ETLogger create(String name) {
        return new ETLogger(name, output)
    }

    ETLogger create(String name, int indent) {
        return new ETLogger(name, output, indent)
    }
}
