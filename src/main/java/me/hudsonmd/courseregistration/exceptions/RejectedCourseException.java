package me.hudsonmd.courseregistration.exceptions;

public class RejectedCourseException extends Exception{
    public final String name;

    public RejectedCourseException(final String name) {this.name = name;}
}
