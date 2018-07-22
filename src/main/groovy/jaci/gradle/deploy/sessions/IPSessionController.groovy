package jaci.gradle.deploy.sessions

import groovy.transform.CompileStatic

@CompileStatic
interface IPSessionController extends SessionController {

    String getHost()
    int getPort()

}