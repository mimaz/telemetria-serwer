package pl.poznan.putmotorsport.telemetria.serwer

class DataBuffer {
  private var map: Map[Int, Value] = Map.empty

  def update(id: Int, entry: DataEntry): Unit =
    map synchronized {
      map += id -> Value(entry)
    }

  def get(id: Int): DataEntry =
    map synchronized {
      val value = map.getOrElse(id, Value())
      val diff = time - value.timestamp

      if (diff > 5000) {
        map -= id
        DataEntry.empty
      } else {
        value.entry
      }
    }

  def getIds: Iterable[Int] =
    map synchronized {
      map.keys
    }

  private def time = System.currentTimeMillis()

  private case class Value(entry: DataEntry = DataEntry.empty) {
    val timestamp: Long = time
  }
}
