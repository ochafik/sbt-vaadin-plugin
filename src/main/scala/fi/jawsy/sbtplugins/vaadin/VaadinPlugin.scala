package fi.jawsy.sbtplugins.vaadin

import sbt._

object VaadinPlugin {

  val VaadinCompileDescription = "Compiles a Vaadin widget set"

}

trait VaadinPlugin extends DefaultWebProject {

  import VaadinPlugin._

  def vaadinWidgetSet: String = "com.vaadin.terminal.gwt.DefaultWidgetSet"
  def vaadinOutputPath: Path = vaadinCompilerOutputPath / "VAADIN" / "widgetsets"

  def vaadinCompilerClass: String = "com.vaadin.tools.WidgetsetCompiler"
  def vaadinCompilerArgs: List[String] = List("-out", vaadinOutputPath.absolutePath, vaadinWidgetSet)
  def vaadinCompilerJvmArgs: List[String] = List("-server", "-Xmx700m", "-Xms300m", "-XX:PermSize=100M", "-XX:MaxPermSize=500M")
  def vaadinCompilerClasspath: PathFinder = compileClasspath +++ mainResourcesOutputPath
  def vaadinCompilerOutputPath = outputPath / "vaadin"
  def autorunVaadinCompile = !vaadinCompilerOutputPath.exists

  private def compileVaadinWidgetSet {
    import Process._
    val cp = vaadinCompilerClasspath.getPaths.mkString(System.getProperty("path.separator"))
    val parts = "java" :: vaadinCompilerJvmArgs ::: List("-classpath", cp, vaadinCompilerClass) ::: vaadinCompilerArgs
    
    if (false)
      parts.mkString(" ") ! log
    else {
      import java.io._
      def pipeStream(in: InputStream, out: PrintStream) = scala.concurrent.ops.spawn {
        val inputStream = new BufferedReader(new InputStreamReader(in))
        var str: String = null
        while ({ str = inputStream.readLine; str != null })
          out.println(str)
      }
      val p = Runtime.getRuntime.exec(cmd.toArray)
      pipeStream(p.getErrorStream, System.err)
      pipeStream(p.getInputStream, System.out)
      if (p.waitFor != 0)
        error("Vaadin compile command failed !")
    }
  }

  lazy val vaadinCompile = vaadinCompileAction
  def vaadinCompileAction = vaadinCompileTask.dependsOn(copyResources) describedAs VaadinCompileDescription
  def vaadinCompileTask = task {
    compileVaadinWidgetSet
    None
  }

  def autoVaadinCompileTask = task {
    if (autorunVaadinCompile) compileVaadinWidgetSet
    None
  } describedAs VaadinCompileDescription

  override def prepareWebappAction = super.prepareWebappAction dependsOn(autoVaadinCompileTask)

  override def extraWebappFiles = super.extraWebappFiles +++ descendents(vaadinCompilerOutputPath ##, "*")

}
