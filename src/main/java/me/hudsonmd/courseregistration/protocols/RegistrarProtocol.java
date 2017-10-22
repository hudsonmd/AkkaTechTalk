package me.hudsonmd.courseregistration.protocols;

import akka.actor.ActorRef;

import java.util.Map;

public class RegistrarProtocol {

    public static class RegisterStudent {
        public final String name;

        public RegisterStudent(final String name) {this.name = name;}
    }

    public static class ProposeCourse {
        public final String name;

        public ProposeCourse(final String name) {
            this.name = name;
        }
    }

    public static class StartRegistration {}

    public static class RequestCourseList {}

    public static class ResponseCourseList {
        public final Map<String, ActorRef> courses;

        public ResponseCourseList(final Map<String, ActorRef> courses) {this.courses = courses;}
    }

    public static class PrintEverything {}
}
