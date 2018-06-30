package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream, IOException}
import java.net.Socket

class TcpConnection(socket: Socket,
                    base: DataBase) extends Thread {
  private val CmdGetData: Int = 1
  private val ResultSuccess: Int = 10

  private var lastTime = System.currentTimeMillis()

  override def run(): Unit = {
    super.run()

    println(s"connection opened with client IP: ${socket.getRemoteSocketAddress}")

    try {
      val dis = new DataInputStream(socket.getInputStream)
      val dos = new DataOutputStream(socket.getOutputStream)

      while (true) {
        val cmd = dis.readInt()

        updateTime()

        cmd match {
          case CmdGetData =>
            handleGetData(dis, dos)

          case _ =>
            handleInvalid(dis, dos)
        }
      }
    } catch {
      case _: IOException => Unit
    }

    try {
      socket.close()
    } catch {
      case _: IOException => Unit
    }

    println("connection closed")
  }

  private def handleGetData(dis: DataInputStream,
                            dos: DataOutputStream): Unit = {
    val maxcnt = dis.readInt()
    val idcnt = dis.readInt()

    var ids: Vector[(Int, Int)] = Vector.empty

    for (_ <- 0 until idcnt) {
      val id = dis.readInt()
      val since = dis.readInt()

      ids :+= (id, since)
    }

    for ((id, since) <- ids) {
      val (newsince, data) =
        try {
          base.request(id, since, maxcnt)
        } catch {
          case _: NoSuchElementException =>
            (since, Vector.empty[DataEntry])
        }

      dos.writeInt(ResultSuccess)
      dos.writeInt(newsince)
      dos.writeInt(data.length)

      for (value <- data)
        value write dos
    }
  }

  private def handleInvalid(dis: DataInputStream,
                            dos: DataOutputStream): Unit = {
    dos.writeInt(0)
    dos.writeUTF("invalid command")
  }

  private def updateTime(): Unit =
    lastTime = System.currentTimeMillis()
}
