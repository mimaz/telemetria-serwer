package pl.poznan.putmotorsport.telemetria.serwer

import java.io.IOException
import java.util.Scanner

import gnu.io.{CommPortIdentifier, NoSuchPortException, SerialPort}

object UartReader {
  def open(conf: Configuration,
           handler: (Int, DataEntry) => Unit): UartReader = {
    var ident =
      try {
        CommPortIdentifier.getPortIdentifier(conf.Serial)
      } catch {
        case _: NoSuchPortException =>
          fallbackPort
      }

    new UartReader(ident, conf, handler)
  }

  private def fallbackPort: CommPortIdentifier = {
    var idents: List[CommPortIdentifier] = Nil
    val identIt = CommPortIdentifier.getPortIdentifiers

    while (identIt.hasMoreElements)
      idents = identIt.nextElement().asInstanceOf[CommPortIdentifier] :: idents

    idents match {
      case Nil =>
        throw new NoSuchPortException()

      case id :: Nil =>
        id

      case list =>
        println(s"${list.length} ports available: ")

        for ((idx, ident) <- (1 to list.length) zip idents)
          println(s"$idx: ${ident.getName}")

        val scanner = new Scanner(Console.in)
        var num = -1

        while (num < 1 || num > list.length) {
          print("enter port number: ")

          num = scanner.nextInt()
        }

        idents(num - 1)
    }
  }
}

@throws[NoSuchPortException]
@throws[IOException]
@throws[ClassCastException]
class UartReader(ident: CommPortIdentifier,
                 conf: Configuration,
                 handler: (Int, DataEntry) => Unit) extends Thread {
  println(s"opening port ${ident.getName}")

  private val serial = ident.open("telemetria-serwer", 1).asInstanceOf[SerialPort]
  private val istream = serial.getInputStream

  serial.setSerialPortParams(
    conf.BaudRate, conf.DataBits,
    conf.StopBits, conf.Parity)

  override def run(): Unit = {
    super.run()

    val builder = new StringBuilder

    while (!isInterrupted)
      try {
        val code = istream.read()

        if (code != -1) {
          code.asInstanceOf[Char] match {
            case c if c > 0x20 =>
              builder.append(c)

            case c if c > 0 =>
              val line = builder.toString()
              builder.clear()

              if (line.length > 7)
                parseLine(line)

            case _ => Unit
          }
        }
      } catch {
        case e: IOException =>
          println(s"error while reading from uart: $e")
          Thread.sleep(1000)
    }

    serial.close()
  }

  private def parseLine(line: String): Unit =
    try {
      val sep1 = line indexOf '='
      val sep2 = line indexOf ';'

      val idStr = line.substring(0, sep1)
      val timeStr = line.substring(sep1 + 1, sep2)
      val valueStr = line.substring(sep2 + 1)

      val id = Integer.parseInt(idStr, 16)
      val value = Integer.parseInt(valueStr, 16).asInstanceOf[Short]

      if (sep2 - sep1 != 7)
        throw new Exception(s"invalid frame size: ${sep2 - sep1}: $line")

      val entry = DataEntry(value)

      handler(id, entry)
    } catch {
      case e: Exception => println(s"exception while parsing: $e")
    }
}
