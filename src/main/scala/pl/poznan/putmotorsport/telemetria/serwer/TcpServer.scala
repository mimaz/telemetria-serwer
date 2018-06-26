package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream, IOException}
import java.net.{ServerSocket, Socket}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object TcpServer {
  val CmdData: Int = 1
  val ResultSuccess: Int = 10
}

class TcpServer(port: Int,
                base: DataBase) extends Thread {
  private val server = new ServerSocket(port)
  private var sockets: List[Socket] = Nil

  override def interrupt(): Unit = {
    super.interrupt()

    try {
      server.close()
    } catch {
      case e: IOException =>
        println("error while closing server socket: " + e)
    }
  }

  override def run(): Unit = {
    super.run()

    while (!isInterrupted) {
      try {
        println("accept..")
        val socket = server.accept()
        val dis = new DataInputStream(socket.getInputStream)
        val dos = new DataOutputStream(socket.getOutputStream)

        sockets synchronized {
          sockets = socket :: sockets
        }

        Future {
          try {
            while (handle(dis, dos))
              {}
          } catch {
            case e: Throwable if !isInterrupted =>
              Console.err.println("handling request failed: " + e)

            case _: Throwable =>
              println("handling request exited")
          }
        }
      } catch {
        case e: IOException =>
          Console.err.println("server error: " + e)
      }
    }
  }

  private def handle(dis: DataInputStream,
                     dos: DataOutputStream): Boolean = {
    val cmd = dis.readInt()

    def data(): Boolean = {
      val since = dis.readInt()
      val maxcnt = dis.readInt()
      val dataid = dis.readInt()

      val (newsince, data) =
        try {
          base.request(dataid, since)
        } catch {
          case _: NoSuchElementException =>
            (since, Vector.empty[DataEntry])
        }

      dos.writeInt(TcpServer.ResultSuccess)
      dos.writeInt(newsince)
      dos.writeInt(data.length)

      for (value <- data)
        value write dos

      true
    }

    def invalid(): Boolean = {
      dos.writeInt(0)
      dos.writeUTF("invalid command: " + cmd)

      true
    }

    def default(): Boolean = false

    cmd match {
      case TcpServer.CmdData => data()
      case 0 => default()
      case _ => invalid()
    }
  }
}
