import akka.actor.{Actor, ActorSystem, Props, ActorRef}
import akka.pattern.{ ask, pipe }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directive.addDirectiveApply
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.util.Timeout
import scala.concurrent.duration._
import akka.http.javadsl.model.HttpResponse
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import scala.concurrent.Future 

object Server {
  
  def main(args: Array[String]) {
    
    implicit val timeout = Timeout(5 seconds)
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()   
    implicit val executionContext = system.dispatcher
     
    val manager = system.actorOf(Props[ClientManagerActor])
     
    val route =   
        get {
          parameter("clientId".as[String]) {id =>
            complete( (manager ? ClientRequest(id)).mapTo[StatusCode])        
          }
    }         
       
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done  
  }
}
