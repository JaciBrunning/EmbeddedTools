package jaci.gradle.deploy.context

import jaci.gradle.deploy.CommandDeployResult
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.sessions.SessionController
import jaci.gradle.deploy.target.location.DeployLocation
import jaci.gradle.log.ETLogger
import spock.lang.Specification
import spock.lang.Subject

class DefaultDeployContextTest extends Specification {

    def session = Mock(SessionController)
    def sublogger = Mock(ETLogger)
    def logger = Mock(ETLogger) {
        push() >> sublogger
    }
    def location = Mock(DeployLocation)
    def workingDir = "rootwd"

    @Subject
    def context = new DefaultDeployContext(session, logger, location, workingDir)

    def "basic getters"() {
        expect:
        context.getController() == session
        context.getLogger() == logger
        context.getWorkingDir() == workingDir
        context.getDeployLocation() == location
    }

    def "Sub Context"() {
        def subdir = "subdir"

        when:
        def subctx = context.subContext(subdir)
        then:
        subctx.getController() == session
        subctx.getLogger() == logger.push()
        subctx.getDeployLocation() == location
        subctx.getWorkingDir() == "rootwd/subdir"
    }

    def "Execute"() {
        when:
        context.execute("cmd")
        then:
        1 * session.execute("mkdir -p $workingDir")
        1 * session.execute("cd $workingDir\ncmd")
        0 * session.execute(_)
    }

    def "Execute without Result"() {
        when:
        def r = context.execute("cmd")
        then:
        r == null
    }

    def "Execute with Result"() {
        def result = Mock(CommandDeployResult)
        session.execute(_) >> result

        when:
        def r = context.execute("cmd")
        then:
        r == result
    }

    def "Put Multiple"() {
        def files = [new File("a"), new File("a/b")] as Set<File>
        def map = [
                ("$workingDir/a".toString()): files[0],
                ("$workingDir/b".toString()): files[1]
        ]

        when:
        context.put(files, null)
        then:
        1 * session.execute("mkdir -p $workingDir")
        1 * session.put(map)
        0 * session.put(_)
    }

    def "Put Single"() {
        def file = new File("a")
        def dest = "dest"
        def map = [
                ("$workingDir/dest".toString()): file
        ]


        when:
        context.put(file, dest, null)
        then:
        1 * session.execute("mkdir -p $workingDir")
        1 * session.put(map)
        0 * session.put(_)
    }

    def "Put with Cache"() {
        def files = [
                "testHit": Mock(File),
                "testMiss": Mock(File)
        ]

        def expectedMiss = [
                ("$workingDir/testMiss".toString()): files["testMiss"]
        ]

        def cm = Mock(CacheMethod) {
            needsUpdate(context, files) >> { ctx, f ->
                ["testMiss"]
            }
            compatible(context) >> true
        }

        when:
        context.put(files, cm)
        then:
        1 * session.put(expectedMiss)
        0 * session.put(_)
    }
}
