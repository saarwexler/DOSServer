package Client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.event.Logging
import scala.concurrent.Future
import akka.pattern.gracefulStop
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

object Main  {
  def main(args: Array[String]) {
     implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
     val numOfClients  =   args(0).toInt 
     val refs = (1 to numOfClients).map {i => 
      system.actorOf(ClientActor.props(i.toString())) }
      println(s"$numOfClients clients has started /\nPress RETURN to stop gracfully...")      
      StdIn.readLine() // let it run until user presses return
       println("Shting down")           
       val fs = refs.map(ref => gracefulStop(ref, 5 seconds, ClientActor.Shutdown));       
       println("bye bye")
  }
  
}


