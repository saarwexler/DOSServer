package Client
import akka.pattern.pipe
import akka.actor.{ Actor, ActorLogging, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.util.Random
import scala.concurrent.duration._
import akka.actor.Cancellable


object ClientActor {
  
  case object Shutdown;
  def props(clientId: String): Props = Props(new ClientActor(clientId))  
}

class ClientActor(clientId: String) extends Actor
  with ActorLogging { 
  
  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val random = new Random
  var timer: Option[Cancellable] =None
  def ScheduleRequest : Cancellable = {
     val delay = random.nextInt(1000)     
    return context.system.scheduler.scheduleOnce(delay milliseconds){
       context.become(waitForResponse)
    http.singleRequest(HttpRequest(uri = s"http://localhost:8080/?clientId=$clientId"))
    .pipeTo(self)
     }
  }
  
  override def preStart() = { 
    timer =Some(ScheduleRequest)      
  }

  def waitForResponse: Receive = {
    case resp @ HttpResponse(code, _, _, _) =>
      log.info(s"got response for client $clientId, response code: $code ")
      resp.discardEntityBytes()
      context.unbecome()
      timer = Some(ScheduleRequest)  
     case ClientActor.Shutdown =>
       context become shuttingdown 
  }
  
  def shuttingdown: Receive = {
      case resp @ HttpResponse(code, _, _, _) =>
      log.info(s"got last response for client $clientId, response code: $code ")
      resp.discardEntityBytes()
       log.info(s"now shutting down client $clientId")
      context stop self
  }
  
  def receive = {
    case ClientActor.Shutdown =>
      for {t <- timer } yield t.cancel()  
      log.info(s"now shutting down client $clientId")
      context stop self
  }

}