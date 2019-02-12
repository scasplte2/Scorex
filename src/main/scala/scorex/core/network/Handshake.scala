package scorex.core.network

import java.net.InetSocketAddress

import scorex.core.app.Version
import scorex.core.network.peer.LocalAddressPeerFeature

case class Handshake(applicationName: String,
                     protocolVersion: Version,
                     nodeName: String,
                     declaredAddress: Option[InetSocketAddress],
                     features: Seq[PeerFeature],
                     time: Long) {

  assert(Option(applicationName).isDefined)
  assert(Option(protocolVersion).isDefined)

  lazy val reachablePeer: Boolean = {
    declaredAddress.isDefined || localAddressOpt.isDefined
  }

  lazy val localAddressOpt: Option[InetSocketAddress] = {
    features.collectFirst { case LocalAddressPeerFeature(addr) => addr }
  }
}
