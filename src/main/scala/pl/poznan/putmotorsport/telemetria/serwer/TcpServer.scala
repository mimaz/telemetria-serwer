package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream, IOException}
import java.net.{ServerSocket, Socket}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object TcpServer {
  val CmdData: Int = 1
  val ResultSuccess: Int = 10
}

class TcpServer(conf: Configuration,
                base: DataBase) extends Thread {
  private val server = new ServerSocket(conf.Port)
  private var sockets: List[Socket] = Nil

  override def interrupt(): Unit = {
    super.interrupt()

    sockets synchronized {
      try {
        server.close()
      } catch {
        case e: IOException =>
          println("error while closing server socket: " + e)
      }

      for (socket <- sockets)
        try {
          socket.close()
        } catch {
          case _: IOException => Unit
        }
    }
  }

  override def run(): Unit = {
    super.run()

    while (!isInterrupted) {
      try {
        val socket = server.accept()

        sockets synchronized {
          sockets = socket :: sockets
        }

        Future {
          try {
            handle(socket)
          } catch {
            case e: Exception if !isInterrupted =>
              Console.err.println("handling request failed: " + e)

            case _: Exception =>
              Unit
          } finally {
            try {
              socket.close()
            } catch {
              case _: IOException => Unit
            }
          }
        }
      } catch {
        case e: IOException =>
          Console.err.println("server error: " + e)
      }
    }
  }

  private def handle(socket: Socket): Unit = {
    val dis = new DataInputStream(socket.getInputStream)
    val dos = new DataOutputStream(socket.getOutputStream)

    def data(): Unit = {
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

        dos.writeInt(TcpServer.ResultSuccess)
        dos.writeInt(newsince)
        dos.writeInt(data.length)

        for (value <- data)
          value write dos
      }
    }

    def invalid(): Unit = {
      dos.writeInt(0)
      dos.writeUTF("invalid command!")
    }

    var cmd = dis.readInt()

    while (cmd != 0) {
      cmd match {
        case TcpServer.CmdData => data()
        case _ => invalid()
      }

      cmd = dis.readInt()
    }
  }
}
