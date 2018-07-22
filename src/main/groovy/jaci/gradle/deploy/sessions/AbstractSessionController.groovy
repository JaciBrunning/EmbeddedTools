package jaci.gradle.deploy.sessions

import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import java.util.concurrent.Semaphore

@CompileStatic
abstract class AbstractSessionController implements SessionController {

    private Semaphore semaphore
    private Logger log
    private int semI

    AbstractSessionController(int maxConcurrent) {
        semaphore = new Semaphore(maxConcurrent)
        semI = 0
    }

    protected int acquire() {
        int sem = semI++;
        getLogger().debug("Acquiring Semaphore " + sem + " (" + semaphore.availablePermits() + " available)")
        long before = System.currentTimeMillis()
        semaphore.acquire()
        long time = System.currentTimeMillis() - before
        getLogger().debug("Semaphore " + sem + " acquired (took " + time + "ms)")
        return sem
    }

    protected void release(int sem) {
        semaphore.release()
        log.debug("Semaphore " + sem + " released")
    }

    protected Logger getLogger() {
        if (log == null) log = Logger.getLogger(toString())
        return log
    }

    @Override
    public String toString() {
        return "${this.class.simpleName}[]"
    }

}
