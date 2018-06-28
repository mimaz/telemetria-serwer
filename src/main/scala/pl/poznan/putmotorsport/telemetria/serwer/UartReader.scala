package pl.poznan.putmotorsport.telemetria.serwer

import java.io.IOException

import gnu.io.{CommPortIdentifier, NoSuchPortException, SerialPort}

@throws[NoSuchPortException]
@throws[IOException]
@throws[ClassCastException]
class UartReader(portName: String,
                 baud: Int) extends Thread {
  private val dataBits = SerialPort.DATABITS_8
  private val stopBits = SerialPort.STOPBITS_1
  private val parity = SerialPort.PARITY_NONE

  private val ident = CommPortIdentifier.getPortIdentifier(portName)
  private val serial = ident.open("telemetria-serwer", 1).asInstanceOf[SerialPort]
  private val istream = serial.getInputStream

  override def run(): Unit = {
    super.run()

    val builder = new StringBuilder

    while (!isInterrupted)
      try {
        val bytes = istream.readAllBytes()

        for (byte <- bytes)
          byte.asInstanceOf[Char] match {
            case c if c > 0x20 =>
              builder.append(c)

            case c if c > 0 =>
              val line = builder.toString()
              builder.clear()

              parseLine(line)

            case _ => Unit
          }
      } catch {
        case e: IOException =>
          println("error while reading from uart: " + e)
          Thread.sleep(1000)
    }

    serial.close()
  }

  private def parseLine(line: String): Unit =
    try {
      println("line: " + line)
    } catch {
      case e: Exception => println("exception while parsing: " + e)
    }
}
