package pl.poznan.putmotorsport.telemetria.serwer

import java.io._

class DataBase(conf: Configuration) {
  private val baseFilename: String = s"${conf.Directory}/data-base.bin"

  private var regMap: Map[Int, DataRegisterer] = Map.empty

  def push(id: Int, value: DataEntry): Unit =
    synchronized {
      val reg = regMap get id match {
        case Some(rg) => rg

        case None =>
          val reg = new DataRegisterer(id, this, conf)
          regMap += id -> reg

          reg
      }

      reg push value
    }

  @throws[NoSuchElementException]
  def request(id: Int, since: Int, maxCount: Int): (Int, Array[DataEntry]) =
    synchronized {
      regMap(id).request(since, maxCount)
    }

  @throws[IOException]
  def write(dos: DataOutputStream): Unit = {
    dos.writeInt(regMap.size)

    for (reg <- regMap.values) {
      dos.writeInt(reg.dataId)
      reg.write(dos)
    }
  }

  @throws[IOException]
  def read(dis: DataInputStream): Unit = {
    val regCount = dis.readInt()

    for (_ <- 0 until regCount) {
      val did = dis.readInt()
      val reg = new DataRegisterer(did, this, conf)

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
}
