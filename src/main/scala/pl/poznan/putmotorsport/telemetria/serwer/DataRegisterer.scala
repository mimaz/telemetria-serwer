package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{DataInputStream, DataOutput, DataOutputStream, IOException}

class DataRegisterer(val dataId: Int,
                     base: DataBase) {
  private var chunk = new DataChunk(dataId, 0)

  def push(value: DataEntry): Unit =
    synchronized {
      try {
        chunk insert value
      } catch {
        case _: DataChunk.FullException =>
          base saveChunk chunk

          chunk = new DataChunk(dataId, chunk.chunkId + 1)
          chunk insert value
      }
    }

  def request(since: Int): (Int, Vector[DataEntry]) =
    synchronized {
      val count = Math.max(valueId - since, 0)
      val vec = (valueIterator take count).toVector

      (valueId, vec)
    }

  def valueId: Int =
    synchronized {
      chunk.valueId
    }

  def valueIterator: Iterator[DataEntry] =
    try {
      new Iterator[DataEntry] {
        private val chkit = chunkIterator map (_.valueIterator)
        private var entit: Iterator[DataEntry] = chkit.next()

        override def hasNext: Boolean =
          entit.hasNext || chkit.hasNext

        override def next(): DataEntry =
          try {
            entit.next()
          } catch {
            case _: NoSuchElementException =>
              entit = chkit.next()
              entit.next()
          }
      }
    } catch {
      case _: NoSuchElementException =>
        Iterator.empty
    }

  def chunkIterator: Iterator[DataChunk] =
    new Iterator[DataChunk] {
      private var id = chunk.chunkId

      override def hasNext: Boolean =
        id == chunk.chunkId || base.hasChunk(dataId, id)

      override def next(): DataChunk = {
        val chk = {
          if (id == chunk.chunkId)
            chunk
          else
            base.loadChunk(dataId, id)
        }

        id -= 1

        chk
      }
    }

  def saveChunk(chunk: DataChunk): Unit =
    base saveChunk chunk

  def hasChunk(chunkId: Int): Boolean =
    base hasChunk (dataId, chunkId)

  @throws[NoSuchElementException]
  def loadChunk(chunkId: Int): DataChunk =
    base loadChunk (dataId, chunkId)

  @throws[IOException]
  def write(dos: DataOutputStream): Unit =
    chunk write dos

  @throws[IOException]
  def read(dis: DataInputStream): Unit =
    chunk = DataChunk read dis
}
