package me.hudsonmd.courseregistration;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import me.hudsonmd.courseregistration.protocols.OutputWorkerProtocol.Print;
import me.hudsonmd.courseregistration.protocols.OutputWorkerProtocol.RequestPrint;
import me.hudsonmd.courseregistration.protocols.OutputWorkerProtocol.RespondPrint;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class OutputWorker extends AbstractLoggingActor {

    private final String               jobName;
    private final Collection<ActorRef> refs;

    public OutputWorker(String jobName, Collection<ActorRef> refs) {
        this.jobName = jobName;
        this.refs = refs;
    }

    public static Props props(String jobName, Collection<ActorRef> refs) {
        return Props.create(OutputWorker.class, jobName, refs);
    }

    @Override
    public void preStart() throws Exception {
        ActorRef self = getSelf();
        refs.parallelStream()
            .forEach((ActorRef ref) -> ref.tell(new RequestPrint(), getSelf()));
        getContext().getSystem()
                    .scheduler()
                    .scheduleOnce(FiniteDuration.create(3, TimeUnit.SECONDS),
                                  self,
                                  new Print(),
                                  getContext().dispatcher(),
                                  getSelf()
                                 );
    }

    @Override
    public Receive createReceive() {
        StringBuilder output = new StringBuilder();
        output.append("\nJob '" + jobName + "' : [");
        return ReceiveBuilder
                .create()
                .match(RespondPrint.class, (RespondPrint message) -> {
                    output.append("\n\t{" + message.output + "}");
                    refs.remove(getSender());
                }).match(Print.class, (Print message) -> {
                    output.append("\n]");
                    output.append("\n\tDid not recieve responses from : [");
                    for (ActorRef ref : refs) {
                        output.append("\n\t{" + ref.path() + "}");
                    }
                    output.append("\n\t]");
                    log().info(output.toString());
                }).build();
    }
}
