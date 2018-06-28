package pl.poznan.putmotorsport.telemetria.serwer

import java.io.IOException
import java.util.Scanner

import gnu.io.SerialPort

import scala.util.Random

object Main {
  def main(args: Array[String]): Unit = {
    val base = new DataBase("/home/mimakkz/telemetria/")
    val server = new TcpServer(8080, base)
    val scanner = new Scanner(Console.in)

    val reader = new UartReader("/dev/ttyACM0", 115200)


    server.start()
    reader.start()


    try {
      base.load()
    } catch {
      case e: IOException =>
        println("loading failed: " + e)
    }



    var running = true

    while (running) {
      print("$< ")

      scanner.nextLine() match {
        case "stop" =>
          running = false

        case "test" =>
          for (i <- base valueIterator 0)
            println(i)
          println("-------------")

        case "gen" =>
          for (i <- 1 to 10)
            base.push(0, DataEntry(i))

        case cmd =>
          println("invalid command: " + cmd)
      }
    }



    try {
      base.save()
    } catch {
      case e: IOException =>
        println("saving failed: " + e)
    }


    server.interrupt()
    server.join()

    reader.interrupt()
    reader.join()
  }
}
