package jaci.gradle.deploy.sessions

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.CommandDeployResult

@CompileStatic
class SshSessionController extends AbstractSessionController implements IPSessionController {

    private Session session
    private String host, user
    private int port, timeout

    SshSessionController(String host, int port, String user, String password, int timeout, int maxConcurrent = 1) {
        super(maxConcurrent)
        this.host = host
        this.port = port
        this.user = user
        this.timeout = timeout

        this.session = EmbeddedTools.jsch.getSession(user, host, port)
        this.session.setPassword(password)

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        this.session.setConfig(config);
    }

    @Override
    void open() {
        getLogger().info("Connecting to session (timeout=" + timeout + ")")
        session.setTimeout(timeout * 1000)
        session.connect(timeout * 1000)
        getLogger().info("Connected!")
    }

    CommandDeployResult execute(String command) {
        int sem = acquire()

        ChannelExec exec = session.openChannel('exec') as ChannelExec
        exec.command = command
        exec.pty = false
        exec.agentForwarding = false

        def is = exec.inputStream
        exec.connect()
        exec.run()
        try {
            return new CommandDeployResult(command, is.text, exec.exitStatus)
        } finally {
            exec.disconnect()
            release(sem)
        }
    }

    void put(Map<String, File> files) {
        int sem = acquire()

        ChannelSftp sftp = session.openChannel('sftp') as ChannelSftp
        sftp.connect()
        try {
            files.each { String dst, File src ->
                sftp.put(src.absolutePath, dst)
            }
        } finally {
            sftp.disconnect()
            release(sem)
        }
    }

    @Override
    void finalize() {
        try {
            session.disconnect()
        } catch (all) { }
    }

    @Override
    void close() throws IOException {
        try {
            session.disconnect()
        } catch (all) { }
    }

    @Override
    String friendlyString() {
        return "$user@$host:$port"
    }

    @Override
    public String toString() {
        return "${this.class.simpleName}[$user@$host:$port]"
    }

    @Override
    String getHost() {
        return this.host
    }

    @Override
    int getPort() {
        return this.port
    }
}