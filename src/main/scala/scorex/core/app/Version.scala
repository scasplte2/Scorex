package scorex.core.app

import scorex.core.serialization.{BytesSerializable, ScorexSerializer}
import scorex.util.serialization._

/**
  * Version of p2p protocol. Node can only process messages of it's version or lower.
  */
case class Version(firstDigit: Byte, secondDigit: Byte, thirdDigit: Byte) extends BytesSerializable {
  override type M = Version

  override def serializer: ScorexSerializer[Version] = ApplicationVersionSerializer
}

object Version {
  def apply(v: String): Version = {
    val splitted = v.split("\\.")
    Version(splitted(0).toByte, splitted(1).toByte, splitted(2).toByte)
  }

  val initial: Version = Version(0, 0, 1)
  val last: Version = Version(0, 0, 1)

}

object ApplicationVersionSerializer extends ScorexSerializer[Version] {
  val SerializedVersionLength: Int = 3


  override def serialize(obj: Version, w: Writer): Unit = {
    w.put(obj.firstDigit)
    w.put(obj.secondDigit)
    w.put(obj.thirdDigit)
  }

  override def parse(r: Reader): Version = {
    Version(
      r.getByte(),
      r.getByte(),
      r.getByte()
    )
  }
}