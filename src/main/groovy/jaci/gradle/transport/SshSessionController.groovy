package jaci.gradle.transport

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools

@CompileStatic
class SshSessionController {

    Session session

    SshSessionController(String host, String user, String password, int timeout) {
        session = EmbeddedTools.jsch.getSession(user, host, 22)
        session.setPassword(password)

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        session.setConfig(config);

        session.setTimeout(timeout*1000)
        session.connect(timeout*1000)
    }

    String execute(String command) {
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
        }
    }

    void put(List<File> sources, List<String> dests) {
        ChannelSftp sftp = session.openChannel('sftp') as ChannelSftp
        try {
            sources.eachWithIndex { File file, int idx ->
                try {
                    sftp.put(file.absolutePath, dests[idx])
                } catch (all) { }
            }
        } finally {
            sftp.disconnect()
        }
    }

    void put(InputStream stream, String dest) {
        ChannelSftp sftp = session.openChannel('sftp') as ChannelSftp
        try {
            sftp.put(stream, dest)
        } finally {
            sftp.disconnect()
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
