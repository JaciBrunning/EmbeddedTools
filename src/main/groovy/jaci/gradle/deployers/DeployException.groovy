package jaci.gradle.deployers

import java.lang.RuntimeException

public class DeployException extends RuntimeException {
    public DeployException() { super() }
    public DeployException(message) { super(message) }
}