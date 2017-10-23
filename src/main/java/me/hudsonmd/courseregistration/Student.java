package me.hudsonmd.courseregistration;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import me.hudsonmd.courseregistration.protocols.CourseProtocol.AddStudent;
import me.hudsonmd.courseregistration.protocols.OutputWorkerProtocol.RequestPrint;
import me.hudsonmd.courseregistration.protocols.OutputWorkerProtocol.RespondPrint;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.RequestCourseList;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.ResponseCourseList;
import me.hudsonmd.courseregistration.protocols.StudentProtocol.AddCourse;
import me.hudsonmd.courseregistration.protocols.StudentProtocol.AddFailed;
import me.hudsonmd.courseregistration.protocols.StudentProtocol.AddSucceeded;
import me.hudsonmd.courseregistration.protocols.StudentProtocol.StartRegistering;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Student extends AbstractActorWithStash {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public final String name;

    public Student(final String name) {
        this.name = name;
    }

    public static Props props(final String name) {
        return Props.create(Student.class, name);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder
                .create()
                .match(StartRegistering.class, (StartRegistering message) -> {
                    log.info(name + " is requesting the course list");
                    Patterns.pipe(Patterns.ask(getContext().getParent(),
                                               new RequestCourseList(),
                                               Timeout.durationToTimeout(FiniteDuration.create(2, TimeUnit.SECONDS))),
                                  getContext().dispatcher()).to(getSelf());
                })
                .match(ResponseCourseList.class, (ResponseCourseList message) -> {
                    log.info(name + " got the course list");
                    ActorRef self = getSelf();
                    message.courses.entrySet()
                                   .parallelStream()
                                   .forEach((Entry<String, ActorRef> course) -> {
                                       if (Math.random() * 10 < 8) {
                                           getSelf().tell(new AddCourse(course.getKey()), self);
                                       }
                                   });
                    getContext().become(registering(message.courses));
                }).build();
    }

    private Receive registering(Map<String, ActorRef> courseMap) {
        unstashAll();
        Set<String> myCourses = new HashSet<>();

        log.info(name + " is beginning to register");

        return ReceiveBuilder
                .create()
                .match(AddCourse.class, (AddCourse message) -> {
                    log.info(name + " is attempting to add class " + message.name);
                    if (courseMap.containsKey(message.name)) {
                        ActorRef courseToAdd = courseMap.get(message.name);

                        Future asked = Patterns.ask(courseToAdd,
                                                    //Message it's sending
                                                    new AddStudent(getSelf()),
                                                    Timeout.durationToTimeout(FiniteDuration.create(2, TimeUnit.SECONDS))
                                                   );

                        Patterns.pipe(asked,getContext().dispatcher())
                                .to(getSelf());

                    }
                }).match(AddSucceeded.class, (AddSucceeded message) -> {
                    log.info(name + " successfully joined " + message.courseName);
                    myCourses.add(message.courseName);
                })
                .match(AddFailed.class, (AddFailed message) -> {
                    log.warning(name + " could not add course " + message.courseName);
                }).match(RequestPrint.class, (RequestPrint message) -> {
                    String output = printString(myCourses);
                    getSender().tell(new RespondPrint(output), getSelf());
                }).build();
    }

    private String printString(Set<String> myCourses) {
        StringBuilder output = new StringBuilder();
        output.append(name + " had the courses : ");
        for (String course : myCourses) {
            output.append("[" + course + "]");
        }
        return output.toString();
    }
}
