package org.kokho.service.chat

/**
 * @author: Mikhail Kokho
 * @date: 7/26/2015.
 */
trait Room {

  def id: String

  def read: Seq[String] = read(1)

  def read(page: Int): Seq[String]

  def post(msg: String): Unit = post("anonymous", msg)

  def post(user: String, msg: String)
}

class MemoryRoom(val id: String) extends Room{
  val PAGE_SIZE = 50
  var messages = Vector[String]()

  override def read(page: Int): Seq[String] =
    messages.drop(PAGE_SIZE * (page - 1)).take(PAGE_SIZE)

  def post(user: String, msg: String): Unit = {
    val formattedMsg  = "%tT %s > %s".format(System.currentTimeMillis(), user, msg)
    messages = formattedMsg +: messages
  }

}