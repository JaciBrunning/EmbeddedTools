package jaci.gradle.transport

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import org.apache.log4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.Semaphore

@CompileStatic
class SshSessionController {

    Session session
    Semaphore semaphore
    Logger log
    int semI

    // TODO Add semaphore locking profiling to build scan
    SshSessionController(String host, int port, String user, String password, int timeout, int maxConcurrent=1) {
        log = Logger.getLogger("SshSessionController " + user + "@" + host + ":" + port)
        session = EmbeddedTools.jsch.getSession(user, host, port)
        session.setPassword(password)

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        session.setConfig(config);

        log.info("Connecting to session (timeout=" + timeout + ")")
        session.setTimeout(timeout*1000)
        session.connect(timeout*1000)
        log.info("Connected!")

        semaphore = new Semaphore(maxConcurrent)
        semI = 0
    }

    int acquire() {
        int sem = semI++;
        log.debug("Acquiring Semaphore " + sem + " (" + semaphore.availablePermits() + " available)")
        long before = System.currentTimeMillis()
        semaphore.acquire()
        long time = System.currentTimeMillis() - before
        log.debug("Semaphore " + sem + " acquired (took " + time + "ms)")
        return sem
    }

    void release(int sem) {
        semaphore.release()
        log.debug("Semaphore " + sem + " released")
    }

    String execute(String command) {
        int sem = acquire()

        ChannelExec exec = session.openChannel('exec') as ChannelExec
        exec.command = command
        exec.pty = false
        exec.agentForwarding = false

        def is = exec.inputStream
        exec.connect()
        exec.run()
        try {
            return is.text
        } finally {
            exec.disconnect()
            release(sem)
        }
    }

    void put(List<File> sources, List<String> dests) {
        int sem = acquire()

        ChannelSftp sftp = session.openChannel('sftp') as ChannelSftp
        sftp.connect()
        try {
            sources.eachWithIndex { File file, int idx ->
                try {
                    sftp.put(file.absolutePath, dests[idx])
                } catch (Exception e) {
                    def s = new StringWriter()
                    def pw = new PrintWriter(s)
                    e.printStackTrace(pw)
                    log.debug("Could not deploy ${file.absolutePath}...")
                    log.debug(s.toString())
                }
            }
        } finally {
            sftp.disconnect()
            release(sem)
        }
    }

    void put(InputStream stream, String dest) {
        int sem = acquire()
        ChannelSftp sftp = session.openChannel('sftp') as ChannelSftp
        try {
            sftp.put(stream, dest)
        } finally {
            sftp.disconnect()
            release(sem)
        }
    }

    void put(File source, String dest) {
        put([source], [dest])
    }

    void disconnect() {
        session.disconnect()
    }

    @Override
    void finalize() {
        try {
            session.disconnect()
        } catch (all) { }
    }
}
