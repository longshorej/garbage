case class FooBar(value: Int)
case class BooBaz(value: Long)

/**
 * Trying to understand how a class registry could be used with Graal/SubstrateVM.
 *
 * $ sbt assembly
 * $ native-image -jar target/scala-2.12/graal-class-registry-assembly-0.1.0-SNAPSHOT.jar
 *
 * $ CLASS_NAME=FooBar ./graal-class-registry-assembly-0.1.0-SNAPSHOT
 *
 * ~/work/garbage/graal-class-registry#master $ CLASS_NAME=FooBar ./graal-class-registry-assembly-0.1.0-SNAPSHOT
 * workingApproach
 * looking up FooBar
 * Some(class FooBar)
 *
 *
 * notWorkingApproach
 * looking up FooBar
 * None
 * -> 0
 */
object App {
  def notWorkingApproach(fqcn: String): Option[Class[_]] = {
    {
      // statically register everything
      Class.forName("FooBar")
      Class.forName("BooBaz")
    }

    println(s"looking up $fqcn")

    try {
      Some(Class.forName(fqcn))
    } catch {
      case _: Throwable => None
    }
  }

  def workingApproach(fqcn: String): Option[Class[_]] = {
    println(s"looking up $fqcn")

    fqcn match {
      case "FooBar" => Some(classOf[FooBar])
      case "BooBaz" => Some(classOf[BooBaz])
      case _ => None
    }
  }

  def main(args: Array[String]): Unit = {
    println("workingApproach")
    println(workingApproach(sys.env.getOrElse("CLASS_NAME", "")))
    println()
    println()
    println("notWorkingApproach")
    println(notWorkingApproach(sys.env.getOrElse("CLASS_NAME", "")))
  }
}
