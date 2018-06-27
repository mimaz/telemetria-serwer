package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{Closeable, IOException, InputStream}

import gnu.io.{CommPortIdentifier, NoSuchPortException, SerialPort}

@throws[NoSuchPortException]
@throws[IOException]
@throws[ClassCastException]
class SerialReader(portName: String,
                   baud: Int,
                   dataBits: Int,
                   stopBits: Int,
                   parity: Int) extends InputStream {
  private val ident = CommPortIdentifier.getPortIdentifier(portName)
  private val serial = ident.open("SerialReader", 2000).asInstanceOf[SerialPort]
  private val istream = serial.getInputStream

  private var buffer: Vector[Byte] = Vector.empty

  serial.setSerialPortParams(baud, dataBits, stopBits, parity)
  serial.notifyOnDataAvailable(true)
  serial.addEventListener(_ => {
    val bytes = istream.readAllBytes()

    buffer synchronized {
      buffer ++= bytes
    }
  })

  override def read(): Int =
    buffer synchronized {
      buffer.headOption match {
        case Some(value) =>
          buffer = buffer.tail
          value

        case None =>
          -1
      }
    }

  override def close(): Unit = {
    super.close()

    serial.close()
  }
}
