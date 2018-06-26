package pl.poznan.putmotorsport.telemetria.serwer

import java.util.Scanner

object Main {
  def main(args: Array[String]): Unit = {
    val base = new DataBase[Int]
    val server = new TcpServer(8080, base)
    val scanner = new Scanner(Console.in)


    server.start()



    var running = true

    while (running) {
      println("$< ")

      scanner.nextLine() match {
        case "stop" =>
          running = false

        case cmd =>
          println("invalid command: " + cmd)
      }
    }


    server.interrupt()
    server.join()
  }
}
