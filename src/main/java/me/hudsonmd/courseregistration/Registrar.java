package me.hudsonmd.courseregistration;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import me.hudsonmd.courseregistration.exceptions.ChangingFinalizedSemesterException;
import me.hudsonmd.courseregistration.exceptions.RejectedCourseException;
import me.hudsonmd.courseregistration.protocols.CourseProtocol.ApprovedCourse;
import me.hudsonmd.courseregistration.protocols.CourseProtocol.CourseDecision;
import me.hudsonmd.courseregistration.protocols.CourseProtocol.RejectedCourse;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.PrintEverything;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.ProposeCourse;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.RegisterStudent;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.RequestCourseList;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.ResponseCourseList;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.StartRegistration;
import me.hudsonmd.courseregistration.protocols.StudentProtocol.StartRegistering;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class Registrar extends AbstractActorWithStash {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private Map<String, ActorRef> courses;
    private Map<String, ActorRef> students;

    public Registrar() {
        courses = new HashMap<>();
        students = new HashMap<>();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(3,
                                     FiniteDuration.create(1, TimeUnit.SECONDS),
                                     DeciderBuilder.match(RejectedCourseException.class,
                                                          (RejectedCourseException e) -> {
                                                              log.warning("Course at " + getSender().path() + " was removed from the registrar");
                                                              courses.remove(e.name);
                                                              return SupervisorStrategy.stop();
                                                          }).build()
        );
    }

    public static Props props() {
        return Props.create(Registrar.class);
    }

    @Override
    public Receive createReceive() {
        return planningSemester();
    }

    private Receive planningSemester() {
        return ReceiveBuilder
                .create()
                .match(RegisterStudent.class, (RegisterStudent message) -> {
                    log.info(("Registering student " + message.name));
                    students.put(message.name, getContext().actorOf(Student.props(message.name),
                                                                    message.name.replace(" ", "_")));
                })
                .match(ProposeCourse.class, (ProposeCourse message) -> {
                    log.info("Course " + message.name + " proposed");
                    ActorRef proposedCourse = getContext().actorOf(Course.props(message.name),
                                                                   message.name.replace(" ", "_"));
                    courses.put(message.name, proposedCourse);
                }).match(StartRegistration.class, (StartRegistration message) -> {
                    log.info("Publishing courses");
                    final ActorRef self = getSelf();

                    courses.entrySet()
                           .parallelStream()
                           .forEach((Entry<String, ActorRef> course) -> {
                               CourseDecision decisionMessage = validateCourse(course.getKey());
                               course.getValue().tell(decisionMessage, self);
                           });
                    students.values()
                            .parallelStream()
                            .forEach((ActorRef student) -> {
                                // could have alternatively used a broadcast router or published to a topic
                                student.tell(new StartRegistering(), self);
                            });

                    getContext().become(finalizedSemester());
                }).build();
    }

    private CourseDecision validateCourse(String courseName) {
        if (courseName.matches("CS\\d{3}[A-Z]{0,1} [A-z ]*")) {
            return new ApprovedCourse((int) (Math.random() * 4 + 4));
        }
        return new RejectedCourse("Invalid course courseName");
    }

    private Receive finalizedSemester() {
        unstashAll();

        log.info("Semester has been finalized");
        return ReceiveBuilder.create()
                             .match(RequestCourseList.class, (RequestCourseList message) -> {
                                 getSender().tell(new ResponseCourseList(courses), getSelf());
                             })
                             .match(PrintEverything.class, (PrintEverything message) -> {
                                 String               jobName = "Printing registrar information";

                                 List<ActorRef>       actorsToPrint = new ArrayList<>();
                                 actorsToPrint.addAll(courses.values());
                                 actorsToPrint.addAll(students.values());

                                 getContext().actorOf(OutputWorker.props(jobName, actorsToPrint));
                             })
                             .matchAny((message) -> {
                                 throw new ChangingFinalizedSemesterException();
                             }).build();
    }
}
