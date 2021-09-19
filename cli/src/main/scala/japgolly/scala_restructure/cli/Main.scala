package japgolly.scala_restructure.cli

object Main {
  def main(args: Array[String]): Unit = {

    for (o <- Options.fromArgs(args)) {
      val app = new App(o.withDefaults)
      app.run()
    }

  }
}
