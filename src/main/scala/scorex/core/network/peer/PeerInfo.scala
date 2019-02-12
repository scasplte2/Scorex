package scorex.core.network.peer

import java.net.InetSocketAddress

import scorex.core.network.{ConnectionType, Handshake}

/**
  * Info about peer to be kept in local storage.
  * Contains handshake with node info, and connection type if connected
  */
case class PeerInfo(handshake: Handshake,
                    connection: Option[(ConnectionType, InetSocketAddress)])
