package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream}

object DataEntry {
  def read(dis: DataInputStream): DataEntry =
    DataEntry(dis.readInt())
}

case class DataEntry(id: Int) {
  def write(dos: DataOutputStream): Unit =
    dos.writeInt(id)
}
