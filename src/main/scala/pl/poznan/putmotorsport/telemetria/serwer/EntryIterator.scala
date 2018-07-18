package pl.poznan.putmotorsport.telemetria.serwer

import java.io.{Closeable, DataInputStream, FileInputStream, IOException}

class EntryIterator(reg: DataRegisterer)
    extends Iterator[DataEntry] with Closeable {

  private val local = reg.localData.iterator
  private var fopt: Option[FileInputStream] = None
  private var vopt: Option[DataEntry] = None
  private var chunk: Int = reg.chunkId - 1

  override def hasNext: Boolean = {
    load()

    vopt.isDefined
  }

  @throws[NoSuchElementException]
  override def next(): DataEntry = {
    load()

    val entry = vopt.get

    vopt = None

    entry
  }

  override def close(): Unit =
    try {
      fopt.get.close()
    } catch {
      case e: IOException =>
        Console.err.println (s"ERROR: closing chunk file faled: $e")

      case _: NoSuchElementException =>
        Unit
    } finally {
      fopt = None
    }

  private def load(): Unit =
    (vopt, fopt, local.hasNext) match {
      case (None, _, true) =>
        vopt = Some(local.next())

      case (None, None, false) =>
        try {
          val fname = reg.chunkName(chunk)
          val fis = new FileInputStream(fname)
          val dis = new DataInputStream(fis)

          fopt = Some(fis)
          chunk -= 1

          val entry = DataEntry.read(dis)

          vopt = Some(entry)
        } catch {
          case _: IOException =>
            Unit
        }

      case (None, Some(fis), false) =>
        try {
          val dis = new DataInputStream(fis)
          val entry = DataEntry.read(dis)

          vopt = Some(entry)
        } catch {
          case _: IOException =>
            close()
            load()
        }

      case (Some(_), _, _) =>
        Unit
    }
}
