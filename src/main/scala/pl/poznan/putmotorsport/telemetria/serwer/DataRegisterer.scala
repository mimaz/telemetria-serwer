package pl.poznan.putmotorsport.telemetria.serwer

object DataRegisterer {
  class FullException extends Exception

  class Chunk[A](val id: Int = 0) {
    private var vector: Vector[A] = Vector.empty

    @throws[FullException]
    def insert(value: A): Unit =
      if (vector.length < Size)
        vector = value +: vector
      else
        throw new FullException

    def valueIterator: Iterator[A] = vector.iterator
  }

  val Size: Int = 4
  val Chunks: Int = 4
}

class DataRegisterer[A](val id: Int) {
  private var chunks: Vector[DataRegisterer.Chunk[A]] = Vector(new DataRegisterer.Chunk[A])
  private var valueid: Int = 0

  def lastChunkId: Int = chunks.last.id

  def push(value: A): Unit = synchronized {
    try {
      chunks.last insert value
    } catch {
      case _: DataRegisterer.FullException =>
        val chunk = new DataRegisterer.Chunk[A](lastChunkId + 1)

        chunks = (chunks :+ chunk) takeRight DataRegisterer.Chunks
        chunks.last insert value
    }

    valueid += 1
  }

  def request(since: Int): (Int, Vector[A]) = synchronized {
    val count = Math.max(valueid - since, 0)
    val vec = (valueIterator take count).toVector

    (valueid, vec)
  }

  def valueIterator: Iterator[A] =
    try {
      new Iterator[A] {
        private val chkit = chunkIterator map (_.valueIterator)
        private var entit: Iterator[A] = chkit.next()

        override def hasNext: Boolean =
          entit.hasNext || chkit.hasNext

        override def next(): A =
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

  def chunkIterator: Iterator[DataRegisterer.Chunk[A]] =
    chunks.reverseIterator
}
