package pl.poznan.putmotorsport.telemetria.serwer

import java.io.IOException
import java.util.Scanner

import scala.util.Random

object Main {
  def main(args: Array[String]): Unit = {
    val base = new DataBase
    val server = new TcpServer(8080, base)
    val scanner = new Scanner(Console.in)


    server.start()


    try {
      base.load()
    } catch {
      case e: IOException =>
        println("loading failed: " + e)
    }

    val rand = new Random(100)



    var running = true

    while (running) {
      println("$< ")

      scanner.nextLine() match {
        case "stop" =>
          running = false

        case "test" =>
          for (i <- base valueIterator 0)
            println(i)
          println("-------------")

        case "gen" =>
          for (i <- 1 to 10)
            base.push(0, DataEntry(rand.nextInt(50)))

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
  }
}
