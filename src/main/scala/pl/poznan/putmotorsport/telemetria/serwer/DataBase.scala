package pl.poznan.putmotorsport.telemetria.serwer

class DataBase[A] {
  private var regMap: Map[Int, DataRegisterer[A]] = Map.empty

  def push(id: Int, value: A): Unit = synchronized {
    val reg = regMap get id match {
      case Some(rg) => rg

      case None =>
        val reg = new DataRegisterer[A](id)
        regMap += id -> reg

        reg
    }

    reg push value
  }

  @throws[NoSuchElementException]
  def request(id: Int, since: Int): (Int, Vector[A]) = synchronized {
    regMap(id) request since
  }

  def valueIterator(id: Int): Iterator[A] =
    try {
      regMap(id).valueIterator
    } catch {
      case _: NoSuchElementException =>
        Iterator.empty
    }
}
