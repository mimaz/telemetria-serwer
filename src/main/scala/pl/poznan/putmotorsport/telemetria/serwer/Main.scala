package pl.poznan.putmotorsport.telemetria.serwer

import java.io.IOException
import java.util.Scanner

object Main {
  def main(args: Array[String]): Unit = {
    val conf = Configuration(args)

    val base = new DataBase(conf)
    val reader = new UartReader(conf, base)
    val server = new TcpServer(conf, base)
    val scanner = new Scanner(Console.in)

    reader.start()
    server.start()


    try {
      base.load()
    } catch {
      case e: IOException =>
        println(s"loading failed: $e")
    }



    var running = true

    while (running) {
      print("$< ")

      scanner.nextLine() match {
        case "stop" =>
          running = false

        case "print" =>
          for (i <- base valueIterator 90)
            println(i)
          println("-------------")

        case cmd =>
          println(s"invalid command: $cmd")
      }
    }



    try {
      base.save()
    } catch {
      case e: IOException =>
        println("saving failed: " + e)
    }

    reader.interrupt()
    reader.join()

    server.interrupt()
    server.join()
  }
}
