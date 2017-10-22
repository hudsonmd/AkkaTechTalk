package me.hudsonmd.courseregistration.protocols;

import akka.actor.ActorRef;

public class CourseProtocol {

    public interface CourseDecision {}

    public static class ApprovedCourse implements CourseDecision {
        public final int capacity;

        public ApprovedCourse(int capacity) {
            this.capacity = capacity;
        }
    }

    public static class RejectedCourse implements CourseDecision {
        public final String reason;

        public RejectedCourse(final String reason) {
            this.reason = reason;
        }
    }

    public static class AddStudent {
        public final ActorRef student;

        public AddStudent(final ActorRef student) {
            this.student = student;
        }
    }

    public static class DropStudent {
        public final ActorRef student;

        public DropStudent(final ActorRef student) {
            this.student = student;
        }
    }
}
