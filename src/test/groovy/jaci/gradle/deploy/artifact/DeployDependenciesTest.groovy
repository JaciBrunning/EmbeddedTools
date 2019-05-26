package jaci.gradle.deploy.artifact

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class DeployDependenciesTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def "Deploy Properly Depends"() {
        given:
        buildFile << """
plugins {
    id 'jaci.gradle.EmbeddedTools'
}

deploy {
    targets {
        target('test') {
            directory = '/home/lvuser'
            locations {
                ssh {
                    address = 'does.not.exist'
                    user = 'no'
                }
            }
        }
    }
    artifacts {
        fileArtifact('myFileArtifact') {
            file = project.file('build.gradle')
            targets << 'test'
        }
        commandArtifact('myCommandArtifact') {
            command = 'uname -a'
            targets << 'test'
        }
    }
}
"""
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('deploy', '--stacktrace', '-Pdeploy-dry')
            .withPluginClasspath()
            .build()
        then:
            result.task(':discoverTest').outcome == SUCCESS
            result.task(':deployMyCommandArtifactTest').outcome == SUCCESS
            result.task(':deployMyFileArtifactTest').outcome == SUCCESS
            result.task(':deploy').outcome == SUCCESS
    }

    def "Deploy Explicit Skips"() {
        given:
        buildFile << """
plugins {
    id 'jaci.gradle.EmbeddedTools'
}

deploy {
    targets {
        target('test') {
            directory = '/home/lvuser'
            locations {
                ssh {
                    address = 'does.not.exist'
                    user = 'no'
                }
            }
        }
    }
    artifacts {
        fileArtifact('myFileArtifact') {
            file = project.file('build.gradle')
            targets << 'test'
        }
        commandArtifact('myCommandArtifact') {
            command = 'uname -a'
            targets << 'test'
            explicit = true
        }
    }
}
"""
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('deploy', '--stacktrace', '-Pdeploy-dry')
            .withPluginClasspath()
            .build()
        then:
            result.task(':discoverTest').outcome == SUCCESS
            result.task(':deployMyCommandArtifactTest') == null
            result.task(':deployMyFileArtifactTest').outcome == SUCCESS
            result.task(':deploy').outcome == SUCCESS
    }
}
