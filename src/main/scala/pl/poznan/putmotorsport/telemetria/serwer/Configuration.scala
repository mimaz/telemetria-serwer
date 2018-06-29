package pl.poznan.putmotorsport.telemetria.serwer

import gnu.io.SerialPort

case class Configuration(args: Array[String]) {
  private val Usage = "Usage: program /path/to/data/directory /dev/serial/device"

  if (args.length < 1)
    throw new Exception(Usage)

  val UpdateInterval: Int = 200
  val BaudRate: Int = 115200
  val DataBits: Int = SerialPort.DATABITS_8
  val StopBits: Int = SerialPort.STOPBITS_1
  val Parity: Int = SerialPort.PARITY_NONE
  val Port: Int = 8080

  val Directory: String = args(0)
  val Serial: String =
    try {
      args(1)
    } catch {
      case _: ArrayIndexOutOfBoundsException => "invalid port"
    }
}
