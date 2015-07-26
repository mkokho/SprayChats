package org.kokho.service.chat


import grizzled.slf4j.Logging
import org.hashids.Hashids

import scala.util.{Failure, Try, Success}

/**
 * @author: Mikhail Kokho
 * @date: 7/26/2015.
 */
trait RoomProvider {

  def iterate: Iterator[Room]

  def enter(id: String): Try[Room]

  def open(): Try[Room]
}

object MemoryRoomProvider extends RoomProvider with Logging{

  private val hasids = Hashids("bla-bla-bla-Batman")

  private def nextId = hasids.encode(System.currentTimeMillis())

  private var rooms: Map[String, Room] = Map.empty

  override def iterate: Iterator[Room] = rooms.valuesIterator

  override def open(): Try[Room] = {
    val newRoom = new MemoryRoom(nextId)
    newRoom.post("Room opened")
    rooms = rooms + (newRoom.id -> newRoom)
    info(s"A room ${newRoom.id} has been opened")
    Success(newRoom)
  }

  override def enter(id: String): Try[Room] = rooms.get(id) match {
    case Some(room) => Success(room)
    case None => Failure(new NoSuchElementException(s"Room $id is not open"))
  }
}
