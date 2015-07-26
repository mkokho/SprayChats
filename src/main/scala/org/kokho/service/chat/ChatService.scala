package org.kokho.service.chat

import akka.actor.{ActorLogging, Actor}
import grizzled.slf4j.Logging
import spray.http.HttpHeaders.Accept
import spray.routing._
import spray.http._
import MediaTypes._

import scala.util.{Failure, Success}

//object SharedRoomProvider extends MemoryRoomProvider

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class ChatServiceActor extends Actor with ChatService with ActorLogging {

  override def roomProvider: RoomProvider = MemoryRoomProvider

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  log

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = {
    runRoute(myRoute)
  }
}


// this trait defines our service behavior independently from the service actor
trait ChatService extends HttpService with Logging {

  def roomProvider: RoomProvider

  def indexPage = {
    <html>
      <body>
        <h1>Open Rooms:</h1>
        <div>
          {roomProvider.iterate map { room =>
          <p>
            <a href={"chat/" + room.id}>
              {room.id}
            </a>
          </p>
        }}
        </div>
        <form method="post" action="chat">
          <input type="submit" value="New Room"/>
        </form>
      </body>
    </html>
  }

  def roomPage(room: Room) =
    <html>
      <body>
        <h1>Room
          {room.id}
        </h1>
        <div>
          <form method="post">
            You >
            <input type="text" name="msg"/>
            <input type="submit" value="Post"/>
          </form>
        </div>
        <div>
          {room.read map { msg =>
          <p>
            {msg}
          </p>
        }}
        </div>
      </body>
    </html>

  val notFound =
    <html>
      <body>
        <h1>Not Found</h1>
      </body>
    </html>

  val test =
    path("a") {
      path("b") {
        path("c") {
          complete("Hello")
          complete("Hello")
        }
      }
    }

  val myRoute =
    path("") {
      info("%ninfo 1".format())
      get {
        respondWithMediaType(`text/html`) {
          complete {
            indexPage
          }
        }
      }
    } ~
      path("chat") {
        info("%ninfo 2".format())
        post {
          headerValueByName("Accept") { `text/html` =>
            val room = roomProvider.open().get
            redirect("chat/" + room.id, StatusCodes.SeeOther)
          } ~
            headerValueByName("Accept") { `text/xml` =>
              val room = roomProvider.open().get
              complete {
                <room>
                  <id>
                    {room.id}
                  </id>
                  <link title="location" href={"chat/" + room.id}/>
                </room>
              }
            }
        }
      } ~
      path("chat" / Segment) { roomId =>
        info("%ninfo 3".format())
        respondWithMediaType(`text/html`) {
          roomProvider.enter(roomId) match {
            case Failure(ex) => complete {
              notFound
            }
            case Success(room) =>
              get {
                complete {
                  roomPage(room)
                }
              } ~
                post {
                  formField('msg) { msg =>
                    room.post(msg)
                    complete {
                      roomPage(room)
                    }
                  }
                }
          }
        }
      }


}


class RouteConcatenation(route: Route) {

  /**
   * Returns a Route that chains two Routes. If the first Route rejects the request the second route is given a
   * chance to act upon the request.
   */
  def ~(other: Route): Route = { ctx ⇒
    route {
      ctx.withRejectionHandling { rejections ⇒
        other(ctx.withRejectionsMapped(rejections ++ _))
      }
    }
  }
}