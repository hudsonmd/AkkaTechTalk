package me.hudsonmd.courseregistration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.PrintEverything;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.ProposeCourse;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.RegisterStudent;
import me.hudsonmd.courseregistration.protocols.RegistrarProtocol.StartRegistration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CourseRegistration {
    private static String[] courses  = {
            "CS327E ELEMENTS OF DATABASES",
            "CS429 COMP ORGANIZATN AND ARCH",
            "CS331 ALGORITHMS AND COMPLEXITY",
            "CS439 PRINCIPLES OF COMPUTER SYS-C S",
            "CS347 Data Management",
            "CS373 Software Engineering",
            "ECO114 Economies at Scale"
    };
    private static String[] students = {
            "Sue Anne",
            "Joe",
            "Thomas",
            "Jeff",
            "Mark",
            "Victoria",
            "Kelsey",
            "Greg",
            "Alex",
            "Tyler",
            "Kevin",
            "Ben"
    };

    public static void main(String[] args) {
        Config      config = ConfigFactory.load();
        ActorSystem system = ActorSystem.create("CourseRegistration", config);

        ActorRef registrar = system.actorOf(Registrar.props(), "registrar");

        Arrays.stream(courses)
              .parallel()
              .forEach((String courseName) -> {
                  registrar.tell(new ProposeCourse(courseName), ActorRef.noSender());
              });

        Arrays.stream(students)
              .parallel()
              .forEach((String studentName) -> {
                  registrar.tell(new RegisterStudent(studentName), ActorRef.noSender());
              });

        system.scheduler()
              .scheduleOnce(FiniteDuration.create(5, TimeUnit.SECONDS),
                            registrar,
                            new StartRegistration(),
                            system.dispatcher(),
                            ActorRef.noSender()
                           );

        system.scheduler()
              .scheduleOnce(FiniteDuration.create(20, TimeUnit.SECONDS),
                            registrar,
                            new PrintEverything(),
                            system.dispatcher(),
                            ActorRef.noSender()
                           );
    }
}
