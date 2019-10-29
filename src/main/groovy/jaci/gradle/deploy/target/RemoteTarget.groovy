package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.discovery.TargetDiscoveryTask
import jaci.gradle.deploy.target.location.DeployLocation
import jaci.gradle.deploy.target.location.DeployLocationSet
import org.apache.log4j.Logger
import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Named
import org.gradle.api.Project
import java.util.function.Function
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskCollection

import javax.inject.Inject

@CompileStatic
class RemoteTarget implements Named {
    private Logger log
    private final String name
    private final Project project

    @Inject
    RemoteTarget(String name, Project project) {
        this.name = name
        this.project = project
        this.dry = EmbeddedTools.isDryRun(project)
        locations = project.objects.newInstance(DeployLocationSet, project, this)
        log = Logger.getLogger(toString())
    }

    String directory        = null     // Null = default user home
    int timeout             = 3
    boolean failOnMissing   = true
    int maxChannels         = 1

    // TODO: Enable this to be called from context
    boolean dry             = false

    DeployLocationSet locations

    Function<DeployContext, Boolean> onlyIf = null  // Delegate: DeployContext

    @Override
    String getName() {
        return name
    }

    Project getProject() {
        return project
    }

    void locations(final Action<DomainObjectCollection<? extends DeployLocation>> action) {
        action.execute(locations)
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
        if (onlyIf == null) {
            return true;
        }

        log.debug("OnlyIf...")
        boolean toConnect = onlyIf.apply(ctx)
        if (!toConnect) {
            log.debug("OnlyIf check failed! Not connecting...")
            return false
        }
        return true
    }
}
