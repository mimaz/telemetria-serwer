package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream}

object DataEntry {
  def read(dis: DataInputStream): DataEntry = {
    val value = dis.readShort()

    DataEntry(value)
  }

  val empty = DataEntry(0)
}

case class DataEntry(value: Short) {
  def write(dos: DataOutputStream): Unit = {
    dos.writeShort(value)
  }

  override def toString: String = s"$value"
}
