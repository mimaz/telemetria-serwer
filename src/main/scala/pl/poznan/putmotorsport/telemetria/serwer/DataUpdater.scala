package pl.poznan.putmotorsport.telemetria.serwer

class DataUpdater(conf: Configuration,
                  buffer: DataBuffer,
                  base: DataBase) extends Thread {
  override def run(): Unit = {
    super.run()

    var nextTime = System.currentTimeMillis()

    while (!isInterrupted) {
      val ids = buffer.getIds

      for (id <- ids) {
        val entry = buffer get id

        base.push(id, entry)
      }


      while (System.currentTimeMillis() < nextTime && !isInterrupted)
        try {
          Thread.sleep(10)
        } catch {
          case _: InterruptedException =>
            interrupt()
        }

      nextTime += conf.UpdateInterval
    }
  }
}
