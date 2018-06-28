package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream}

object DataEntry {
  def read(dis: DataInputStream): DataEntry = {
    val time = dis.readInt()
    val value = dis.readInt()

    DataEntry(time, value)
  }
}

case class DataEntry(time: Int,
                     value: Int) {
  def write(dos: DataOutputStream): Unit = {
    dos.writeInt(time)
    dos.writeInt(value)
  }

  override def toString: String = s"$time :: $value"
}
