package BIDMach.allreduce

import akka.actor.{Actor,ActorRef,Props,Address,ActorSystem,ActorSelection}
import akka.cluster.{Cluster,MemberStatus}	
import akka.cluster.ClusterEvent._
import com.typesafe.config.ConfigFactory
import akka.event.Logging
import akka.actor.ActorLogging
import TestActor._


class TestActor extends Actor with ActorLogging {
    var v = 0;
    def receive = {
    case x:SendTo => {
	    val sel = context.actorSelection(x.dest);
	    sel ! new SendVal(x.v);
	}
    case x:SendVal => {
	    val m = new RecvVal(x.v * 2);
	    println("%s got a sendval msg %d from %s" format (self, x.v, sender().toString));
	    sender ! m;
	}
    case x:RecvVal => {
    	    println("%s got a recval msg %d from %s" format (self, x.v, sender().toString));
	    v = x.v;
	}
    case x:GetVal => {
	    x.v = v;
	}
    case _ => {}
    }
}


object TestActor {

case class SendVal(val v:Int) {}

case class RecvVal(val v:Int) {}

case class SendTo(val dest:String, val v:Int) {}

case class GetVal() {
    var v = 0;
}

def startup(ports: Seq[String]) = {
    ports map { port =>
		// Override the configuration of the port
		val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
		withFallback(ConfigFactory.load())
		
		// Create an Akka system
		val system = ActorSystem("ClusterSystem", config)
		// Create an actor that handles cluster domain events
		system.actorOf(Props[TestActor], name = "testActor")
    }
}

}