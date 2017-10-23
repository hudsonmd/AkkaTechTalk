package me.hudsonmd.courseregistration;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import me.hudsonmd.courseregistration.exceptions.RejectedCourseException;
import me.hudsonmd.courseregistration.protocols.CourseProtocol.AddStudent;
import me.hudsonmd.courseregistration.protocols.CourseProtocol.ApprovedCourse;
import me.hudsonmd.courseregistration.protocols.CourseProtocol.RejectedCourse;
import me.hudsonmd.courseregistration.protocols.OutputWorkerProtocol.RequestPrint;
import me.hudsonmd.courseregistration.protocols.OutputWorkerProtocol.RespondPrint;
import me.hudsonmd.courseregistration.protocols.StudentProtocol.AddFailed;
import me.hudsonmd.courseregistration.protocols.StudentProtocol.AddSucceeded;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Course extends AbstractActorWithStash {
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);


    private final String        name;
    private       Set<ActorRef> students;


    public Course(String name) {this.name = name;}

    public static Props props(String name) {
        return Props.create(Course.class, name);
    }

    @Override
    public void preStart() throws Exception {
        students = new HashSet<>();
    }

    @Override
    public Receive createReceive() {
        return proposedToRegistrar();
    }

    private Receive proposedToRegistrar() {
        return ReceiveBuilder
                .create()
                .match(ApprovedCourse.class, (ApprovedCourse message) -> {
                    log.info(name + " was approved");
                    getContext().become(openForRegistration(message.capacity));
                }).match(RejectedCourse.class, (RejectedCourse message) -> {
                    log.info(name + " was rejected because " + message.reason);
                    throw new RejectedCourseException(name);
                }).build();
    }

    private Receive openForRegistration(final int capacity) {
        unstashAll();
        log.info(name + " is open for registration with " + capacity + " spots");
        if (capacity < 1) {
            getContext().become(atCapacity());
        }
        return ReceiveBuilder
                .create()
                .match(AddStudent.class, (AddStudent message) -> {
                    log.info(name + " is attempting to add student " + message.student);
                    if (!students.contains(message.student)) {
                        students.add(message.student);
                        getSender().tell(new AddSucceeded(name), getSelf());
                    }
                    if (students.size() >= capacity) {
                        getContext().become(atCapacity());
                    }
                }).match(RequestPrint.class, (RequestPrint message) -> {
                    String output = "Course " + name + " has " + (capacity - students.size()) + " spots open";
                    getSender().tell(new RespondPrint(output), getSelf());
                }).build();
    }

    private Receive atCapacity() {
        unstashAll();
        log.info(name + " is now at capacity");
        return ReceiveBuilder
                .create()
                .match(AddStudent.class, (AddStudent message) -> {
                    log.info(name + " is full and cannot add student " + message.student);
                    getSender().tell(new AddFailed(name), self());
                }).match(RequestPrint.class, (RequestPrint message) -> {
                    String output = "Course " + name + " has 0 spots open";
                    getSender().tell(new RespondPrint(output), getSelf());
                }).build();
    }
}
