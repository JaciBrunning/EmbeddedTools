package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.discovery.TargetDiscoveryTask
import jaci.gradle.deploy.target.location.DeployLocation
import jaci.gradle.deploy.target.location.DeployLocationSet
import org.apache.log4j.Logger
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.tasks.TaskCollection
import org.gradle.util.ConfigureUtil

@CompileStatic
class RemoteTarget implements Named {
    private Logger log
    private final String name
    private final Project project

    RemoteTarget(String name, Project project) {
        this.name = name
        this.project = project
        this.dry = EmbeddedTools.isDryRun(project)
        log = Logger.getLogger(toString())
    }

    String directory        = null     // Null = default user home
    int timeout             = 3
    boolean failOnMissing   = true
    int maxChannels         = 1

    // TODO: Enable this to be called from context
    boolean dry             = false

    DomainObjectSet<DeployLocation> locations = new DeployLocationSet(this)

    Closure<Boolean> onlyIf = null  // Delegate: DeployContext

    @Override
    String getName() {
        return name
    }

    Project getProject() {
        return project
    }

    void locations(final Closure closure) {
        ConfigureUtil.configure(closure, (Object)locations)
    }

    TaskCollection<TargetDiscoveryTask> getDiscoveryTask() {
        return project.tasks.withType(TargetDiscoveryTask).matching { TargetDiscoveryTask t ->
            t.target == this
        }
    }

    @Override
    String toString() {
        return "RemoteTarget[${name}]".toString()
    }

    boolean verify(DeployContext ctx) {
        if (onlyIf instanceof Closure) {
            log.debug("OnlyIf...")
            boolean toConnect = ClosureUtils.delegateCall(ctx, onlyIf)
            if (!toConnect) {
                log.debug("OnlyIf check failed! Not connecting...")
                return false
            }
        }
        return true
    }
}