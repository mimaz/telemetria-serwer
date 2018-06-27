package pl.poznan.putmotorsport.telemetria.serwer

import java.io._

class DataBase(val directory: String) {
  private val baseFilename: String = stripPath("base.bin")
  private def chunkFilename(dataId: Int, chunkId: Int): String =
    stripPath("chunk-" + dataId + ":" + chunkId + ".bin")

  private var chunkSet: Set[(Int, Int)] = Set.empty
  private var regMap: Map[Int, DataRegisterer] = Map.empty

  def push(id: Int, value: DataEntry): Unit = synchronized {
    val reg = regMap get id match {
      case Some(rg) => rg

      case None =>
        val reg = new DataRegisterer(id, this)
        regMap += id -> reg

        reg
    }

    reg push value
  }

  @throws[NoSuchElementException]
  def request(id: Int, since: Int): (Int, Vector[DataEntry]) = synchronized {
    regMap(id) request since
  }

  def valueIterator(id: Int): Iterator[DataEntry] =
    try {
      regMap(id).valueIterator
    } catch {
      case _: NoSuchElementException =>
        Iterator.empty
    }

  @throws[IOException]
  def saveChunk(chunk: DataChunk): Unit = {
    val path = chunkFilename(chunk.dataId, chunk.chunkId)
    val fos = new FileOutputStream(path)

    try {
      val dos = new DataOutputStream(fos)

      chunk.write(dos)
    } finally {
      fos.close()
    }

    chunkSet += chunk.dataId -> chunk.chunkId
  }

  def hasChunk(dataId: Int, chunkId: Int): Boolean =
    chunkSet contains (dataId, chunkId)

  @throws[NoSuchElementException]
  @throws[IOException]
  def loadChunk(dataId: Int, chunkId: Int): DataChunk = {
    if (!hasChunk(dataId, chunkId))
      throw new NoSuchElementException

    val path = chunkFilename(dataId, chunkId)
    val fis = new FileInputStream(path)

    val chunk =
      try {
        val dis = new DataInputStream(fis)

        DataChunk.read(dis);
      } finally {
        fis.close()
      }

    println("chunk " + path + " has been loaded")

    chunk
  }

  @throws[IOException]
  def write(dos: DataOutputStream): Unit = {
    dos.writeInt(chunkSet.size)

    for ((did, cid) <- chunkSet) {
      dos.writeInt(did)
      dos.writeInt(cid)
    }

    dos.writeInt(regMap.size)

    for (reg <- regMap.values) {
      dos.writeInt(reg.dataId)
      reg.write(dos)
    }
  }

  @throws[IOException]
  def read(dis: DataInputStream): Unit = {
    val chunkCount = dis.readInt()

    for (_ <- 0 until chunkCount) {
      val did = dis.readInt()
      val cid = dis.readInt()

      chunkSet += did -> cid
    }

    val regCount = dis.readInt()

    for (_ <- 0 until regCount) {
      val did = dis.readInt()
      val reg = new DataRegisterer(did, this)

      reg.read(dis)

      regMap += did -> reg
    }
  }

  @throws[IOException]
  def save(): Unit = {
    val fos = new FileOutputStream(baseFilename)

    try {
      val dos = new DataOutputStream(fos)

      write(dos)
    } finally {
      fos.close()
    }
  }

  @throws[IOException]
  def load(): Unit = {
    val fis = new FileInputStream(baseFilename)

    try {
      val dis = new DataInputStream(fis)

      read(dis)
    } finally {
      fis.close()
    }
  }

  def stripPath(filename: String): String =
    directory + "/" + filename
}
