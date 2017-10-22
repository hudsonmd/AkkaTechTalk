package me.hudsonmd.courseregistration.protocols;

import akka.actor.ActorRef;

public class OutputWorkerProtocol {
    public static class RequestPrint {}
    public static class RespondPrint {
        public final String output;

        public RespondPrint(final String output) {this.output = output;}
    }

    public static class Print{}
}
