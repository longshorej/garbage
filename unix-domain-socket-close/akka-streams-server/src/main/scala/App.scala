import akka.actor._
import akka.stream._
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl._
import akka.util.ByteString
import java.io.File
import scala.io.StdIn

object App {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    import system.dispatcher

    val file = new File("/tmp/garbage-socket")
    file.delete()

    val logic = Flow[ByteString]

    val binding = UnixDomainSocket()
      .bind(file, halfClose = false)
      .toMat(Sink.foreach { connection =>
        println("handling client")
        connection.handleWith(Flow.fromFunction(identity))
      })(Keep.left).run

    StdIn.readLine()

    System.exit(0)
  }
}
