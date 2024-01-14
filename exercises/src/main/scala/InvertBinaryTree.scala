sealed trait Trampoline[A] {
  def result: A = run(this)

  @annotation.tailrec
  private def run(current: Trampoline[A]): A = current match {
    case Trampoline.Done(value)   => value
    case Trampoline.Bounce(value) => run(value())
    case Trampoline.FlatMap(value, cont) => value match {
      case Trampoline.Done(v) => run(cont(v))
      case Trampoline.Bounce(v) => run(Trampoline.FlatMap(v(), cont))
      case Trampoline.FlatMap(v2, cont2) => run(Trampoline.FlatMap(v2, (x: Any) => Trampoline.FlatMap(cont2(x), cont)))
    }
  }
}

object Trampoline {
  case class Done[A](value: A) extends Trampoline[A]
  case class Bounce[A](value: () => Trampoline[A]) extends Trampoline[A]
  case class FlatMap[A, B](value: Trampoline[A], cont: A => Trampoline[B]) extends Trampoline[B]
}


case class Node[A](value: A, left: Option[Node[A]], right: Option[Node[A]])
                  (implicit ord: Ordering[A]) {
  def contains(test: A): Boolean = containsImpl(test).result

  private def containsImpl(test: A): Trampoline[Boolean] =
    if (ord.equiv(value, test))
      Trampoline.Done(true)
    else if (left.isDefined && right.isDefined)
      Trampoline.FlatMap(left.get.containsImpl(test), (result: Boolean) => if (result) Trampoline.Done(true) else right.get.containsImpl(test))
    else if (left.isDefined)
      left.get.containsImpl(test)
    else if (right.isDefined)
      right.get.containsImpl(test)
    else
      Trampoline.Done(false)

  def depth: Int = depthImpl.result

  private def depthImpl: Trampoline[Int] =
    if (left.nonEmpty && right.nonEmpty)
      Trampoline.FlatMap(left.get.depthImpl, (lX: Int) => Trampoline.FlatMap(right.get.depthImpl, (rX: Int) => Trampoline.Done(1 + Math.max(lX, rX))))
    else if (left.nonEmpty)
      Trampoline.FlatMap(left.get.depthImpl, (x: Int) => Trampoline.Done(x + 1))
    else if (right.nonEmpty)
      Trampoline.FlatMap(right.get.depthImpl, (x: Int) => Trampoline.Done(x + 1))
    else
      Trampoline.Done(1)

  def push(newValue: A): Node[A] = {
    val ret = if (ord.equiv(newValue, value))
      this
    else if (ord.lt(newValue, value))
      copy(left = Some(left match {
        case Some(l) => l.push(newValue)
        case None    => Node(newValue, None, None)
      }))
    else
      copy(right = Some(right match {
        case Some(r) => r.push(newValue)
        case None    => Node(newValue, None, None)
      }))
    ret
  }
}

object Node {
  def apply[A](value: A)(implicit ord: Ordering[A]): Node[A] = Node(value, None, None)
}

object InvertBinaryTree {
  def main(args: Array[String]): Unit = {
    val node = Node(40).push(18).push(7).push(88).push(83)

    println(node.contains(18))
    println(s"depth=${node.depth}")
  }
}
