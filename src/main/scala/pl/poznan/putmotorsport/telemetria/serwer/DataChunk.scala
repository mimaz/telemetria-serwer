package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream, IOException}

object DataChunk {
  class FullException extends Exception

  @throws[IOException]
  def read(dis: DataInputStream): DataChunk = {
    val did = dis.readInt()
    val cid = dis.readInt()
    val len = dis.readInt()

    var vec: Vector[DataEntry] = Vector.empty

    for (_ <- 0 until len)
      vec :+= DataEntry read dis

    new DataChunk(did, cid, vec)
  }

  val Size: Int = 5000
}

class DataChunk(val dataId: Int,
                val chunkId: Int) {
  private var vector: Vector[DataEntry] = Vector.empty

  val stringId: String = dataId + ":" + chunkId

  def this(did: Int, cid: Int, vls: Iterable[DataEntry]) = {
    this(did, cid)

    vector = vls.take(DataChunk.Size).toVector
  }

  @throws[DataChunk.FullException]
  def insert(value: DataEntry): Unit =
    if (vector.length < DataChunk.Size)
      vector = value +: vector
    else
      throw new DataChunk.FullException

  def size: Int = vector.length

  def valueId: Int = chunkId * DataChunk.Size + size

  def valueIterator: Iterator[DataEntry] = vector.iterator

  @throws[IOException]
  def write(dos: DataOutputStream): Unit = {
    dos.writeInt(dataId)
    dos.writeInt(chunkId)
    dos.writeInt(vector.length)

    for (en <- vector)
      en write dos
  }
}
