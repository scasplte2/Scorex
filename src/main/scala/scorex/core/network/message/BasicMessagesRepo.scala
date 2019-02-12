package scorex.core.network.message

import scorex.core.consensus.SyncInfo
import scorex.core.network.message.Message.MessageCode
import scorex.core.network.peer.PeerInfo
import scorex.core.serialization.ScorexSerializer
import scorex.core.{ModifierTypeId, NodeViewModifier}
import scorex.util.Extensions._
import scorex.util.serialization._
import scorex.util.{ModifierId, ScorexLogging, bytesToId, idToBytes}

object BasicMsgDataTypes {
  type InvData = (ModifierTypeId, Seq[ModifierId])
  type ModifiersData = (ModifierTypeId, Map[ModifierId, Array[Byte]])
}

import scorex.core.network.message.BasicMsgDataTypes._

class SyncInfoMessageSpec[SI <: SyncInfo](serializer: ScorexSerializer[SI]) extends MessageSpecV1[SI] {

  override val messageCode: MessageCode = 65: Byte
  override val messageName: String = "Sync"


  override def serialize(data: SI, w: Writer): Unit = {
    serializer.serialize(data, w)
  }

  override def parse(r: Reader): SI = {
    serializer.parse(r)
  }
}

object InvSpec {
  val MessageCode: Byte = 55
  val MessageName: String = "Inv"
}

class InvSpec(maxInvObjects: Int) extends MessageSpecV1[InvData] {

  import InvSpec._

  override val messageCode: MessageCode = MessageCode
  override val messageName: String = MessageName

  override def serialize(data: InvData, w: Writer): Unit = {
    val (typeId, elems) = data
    require(elems.nonEmpty, "empty inv list")
    require(elems.lengthCompare(maxInvObjects) <= 0, s"more invs than $maxInvObjects in a message")
    w.put(typeId)
    w.putUInt(elems.size)
    elems.foreach { id =>
      val bytes = idToBytes(id)
      assert(bytes.length == NodeViewModifier.ModifierIdSize)
      w.putBytes(bytes)
    }
  }

  override def parse(r: Reader): InvData = {
    val typeId = ModifierTypeId @@ r.getByte()
    val count = r.getUInt().toIntExact
    require(count > 0, "empty inv list")
    require(count <= maxInvObjects, s"$count elements in a message while limit is $maxInvObjects")
    val elems = (0 until count).map { c =>
      bytesToId(r.getBytes(NodeViewModifier.ModifierIdSize))
    }

    typeId -> elems
  }
}

object RequestModifierSpec {
  val MessageCode: MessageCode = 22: Byte
  val MessageName: String = "RequestModifier"
}

class RequestModifierSpec(maxInvObjects: Int) extends MessageSpecV1[InvData] {

  import RequestModifierSpec._

  override val messageCode: MessageCode = MessageCode
  override val messageName: String = MessageName

  private val invSpec = new InvSpec(maxInvObjects)


  override def serialize(data: InvData, w: Writer): Unit = {
    invSpec.serialize(data, w)
  }

  override def parse(r: Reader): InvData = {
    invSpec.parse(r)
  }
}

object ModifiersSpec {
  val MessageCode: MessageCode = 33: Byte
  val MessageName: String = "Modifier"
}

class ModifiersSpec(maxMessageSize: Int) extends MessageSpecV1[ModifiersData] with ScorexLogging {

  import ModifiersSpec._

  override val messageCode: MessageCode = MessageCode
  override val messageName: String = MessageName

  override def serialize(data: ModifiersData, w: Writer): Unit = {

    val (typeId, modifiers) = data
    require(modifiers.nonEmpty, "empty modifiers list")

    val (msgCount, msgSize) = modifiers.foldLeft((0, 5)) { case ((c, s), (id, modifier)) =>
      val size = s + NodeViewModifier.ModifierIdSize + 4 + modifier.length
      val count = if (size <= maxMessageSize) c + 1 else c
      count -> size
    }

    val start = w.length()
    w.put(typeId)
    w.putUInt(msgCount)

    modifiers.take(msgCount).foreach { case (id, modifier) =>
      w.putBytes(idToBytes(id))
      w.putUInt(modifier.length)
      w.putBytes(modifier)
    }

    if (msgSize > maxMessageSize) {
      log.warn(s"Message with modifiers ${modifiers.keySet} have size $msgSize exceeding limit $maxMessageSize." +
        s" Sending ${w.length() - start} bytes instead")
    }
  }

  override def parse(r: Reader): ModifiersData = {
    val typeId = ModifierTypeId @@ r.getByte()
    val count = r.getUInt().toIntExact
    val seq = (0 until count).map { _ =>
      val id = bytesToId(r.getBytes(NodeViewModifier.ModifierIdSize))
      val objBytesCnt = r.getUInt().toIntExact
      val obj = r.getBytes(objBytesCnt)
      id -> obj
    }
    (typeId, seq.toMap)
  }
}

object GetPeersSpec extends MessageSpecV1[Unit] {
  override val messageCode: Message.MessageCode = 1: Byte

  override val messageName: String = "GetPeers message"

  override def serialize(obj: Unit, w: Writer): Unit = {
  }

  override def parse(r: Reader): Unit = {
    require(r.remaining == 0, "Non-empty data for GetPeers")
  }
}

object PeersSpec extends MessageSpecV1[Seq[PeerInfo]] {
  private val AddressLength = 4

  override val messageCode: Message.MessageCode = 2: Byte

  override val messageName: String = "Peers message"

  override def parse(r: Reader): Seq[PeerInfo] = ???

  override def serialize(obj: Seq[PeerInfo], w: Writer): Unit = ???
}
