package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutputStream, FileOutputStream, IOException}

class DataRegisterer(val dataId: Int,
                     base: DataBase,
                     conf: Configuration) {
  private var list: List[DataEntry] = Nil
  private var count: Int = 0
  private var since: Int = 0
  private var chunk: Int = 0

  def push(value: DataEntry): Unit =
    synchronized {
      list = value :: list
      count += 1
      since += 1

      if (count > conf.ChunkSize)
        try {
          writeChunk(chunk)

          list = Nil
          count = 0
          chunk += 1
        } catch {
          case _: IOException => Unit
        }
    }

  def request(since: Int, maxCount: Int): (Int, Array[DataEntry]) =
    synchronized {
      //print(s"request for $maxCount from $since with data id ${this.since}")

      val it = new EntryIterator(this)

      val count = Math.min(Math.max(valueId - since, 0), maxCount)
      val vec = (it take count).toArray

      //println(s" :: vec size ${vec.length}")

      it.close()

      (valueId, vec)
    }

  def valueId: Int = since

  @throws[IOException]
  def write(dos: DataOutputStream): Unit =
    synchronized {
      dos.writeInt(count)

      for (entry <- list)
        entry.write(dos)

      dos.writeInt(since)
      dos.writeInt(chunk)
    }

  @throws[IOException]
  def read(dis: DataInputStream): Unit =
    synchronized {
      count = dis.readInt()

      list = (1 to count).map(_ => DataEntry.read(dis)).toList

      since = dis.readInt()
      chunk = dis.readInt()
    }

  def chunkName(id: Int): String =
    s"${conf.Directory}/data-$dataId-$id.bin"

  def chunkId: Int =
    chunk

  def localData: List[DataEntry] =
    list

  private def writeChunk(id: Int): Unit =
    try {
      val fname = chunkName(id)
      val fos = new FileOutputStream(fname)
      val dos = new DataOutputStream(fos)

      for (entry <- list)
        entry.write(dos)
    } catch {
      case e: IOException =>
        Console.err.println(s"ERROR: cannot write file to data base: $e")

        throw e
    }
}
