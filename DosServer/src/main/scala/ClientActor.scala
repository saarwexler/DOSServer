
import akka.actor.{ Actor,ActorRef, FSM }
import scala.concurrent.duration._
import akka.actor.Props
import scala.collection.mutable.Map
import akka.http.scaladsl.model.StatusCodes

sealed trait State
final case object Idle extends State
final case object Active extends State
final case object Blocked extends State

final case class ClientRequest(id: String)
final case object Restart 

class ClientActor extends FSM[State, Int] {
  startWith(Idle, 0)
  
  when (Idle) {
    case Event(ClientRequest(id), _) =>
      goto(Active) using 1 replying StatusCodes.OK
  }
  
  when (Active) {   
    case Event(ClientRequest(_), 4)   =>
      goto(Blocked) using 5 replying StatusCodes.OK
    case Event (ClientRequest(_), i) =>
      stay using i+1   replying StatusCodes.OK 
  }
  when (Blocked) {
    case Event (ClientRequest(_), _) =>
      stay replying StatusCodes.ServiceUnavailable      
  }
  
  whenUnhandled {
    case Event(Restart, _) =>
      goto(Idle) using 0
  }
  
  onTransition {
    case Idle -> Active =>
       setTimer("DOS", Restart, 5 seconds, false) 
  }
  
  initialize()  
}


class ClientManagerActor extends Actor {
  def receive ={
    case msg: ClientRequest => getClientActor(msg.id) forward msg
   // case ClientRequest(clientId) => sender ! StatusCodes.ServiceUnavailable
   // case   ClientRequest(clientId) => getClientActor(clientId) ! new ClientRequest(clientId) 
  }
  
  private val actorCollection = Map[String, ActorRef]()
  def getClientActor(clientId: String): ActorRef =
     actorCollection.getOrElseUpdate(clientId , createClientActor(clientId))
     
     def createClientActor(clientId: String) : ActorRef = 
       context.actorOf(Props[ClientActor])  
}