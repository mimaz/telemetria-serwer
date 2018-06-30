package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream, IOException}
import java.net.{ServerSocket, Socket}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object TcpServer {
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

      sockets = Nil
    }
  }

  override def run(): Unit = {
    super.run()

    while (!isInterrupted) {
      try {
        val socket = server.accept()

        sockets synchronized {
          sockets = socket :: sockets filterNot (s => s.isClosed)
        }

        new TcpConnection(socket, base).start()
      } catch {
        case e: IOException =>
          Console.err.println("server error: " + e)
      }
    }
  }
}
