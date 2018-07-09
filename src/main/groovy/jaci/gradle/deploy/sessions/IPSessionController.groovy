package jaci.gradle.deploy.sessions

interface IPSessionController extends SessionController {

    String getHost()
    int getPort()

}