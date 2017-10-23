package me.hudsonmd.courseregistration.protocols;

public class StudentProtocol {
    public static class StartRegistering {}

    public static class AddCourse {
        public final String name;

        public AddCourse(final String name) {
            this.name = name;
        }
    }

    public static class AddFailed {
        public final String courseName;

        public AddFailed(final String courseName) {
            this.courseName = courseName;
        }
    }

    public static class AddSucceeded {
        public final String courseName;

        public AddSucceeded(final String courseName) {
            this.courseName = courseName;
        }
    }
}
