package org.kokho.service.chat

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._

import scala.util.Random

class ChatServiceSpec extends Specification with Specs2RouteTest with ChatService  {
  def actorRefFactory = system

  def roomProvider = MemoryRoomProvider

  "ChatService" should {

    "return list of open rooms " in {
      Get() ~> myRoute ~> check {
        responseAs[String] must contain("Open Rooms")
      }
    }

    "open a new chat room" in {
      val countRooms = roomProvider.iterate.size
      Get("/chat") ~> myRoute ~> check {
//        responseAs[String] must contain("Room opened")
        roomProvider.iterate.size shouldEqual(countRooms + 1)
      }
    }

    "post a message to a chat room" in {
      val newRoom = roomProvider.open().get
      val msg = "some random message " + Random.nextInt()
      val req = Post("/chat/" + newRoom.id, FormData(Seq("msg" -> msg)))
      req ~> myRoute ~> check {
        responseAs[String] must contain(msg)
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> myRoute ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put() ~> sealRoute(myRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }

}
