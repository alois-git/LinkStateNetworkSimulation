/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linkStateRouting;

import static dijkstra.Dijkstra.getShortestPathTo;
import dijkstra.Edge;
import dijkstra.Vertex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import reso.common.AbstractApplication;
import reso.common.Interface;
import reso.common.InterfaceAttrListener;
import reso.ip.Datagram;
import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPInterfaceListener;
import reso.ip.IPLayer;
import reso.ip.IPLoopbackAdapter;
import reso.ip.IPRouter;

/**
 *
 * @author alo
 */
public class LinkStateRoutingProtocol extends AbstractApplication
        implements IPInterfaceListener, InterfaceAttrListener {

    public static final String PROTOCOL_NAME = "LS_ROUTING";
    public static final int IP_PROTO_LS = Datagram.allocateProtocolNumber(PROTOCOL_NAME);

    public Map<IPAddress, LinkStateMessage> LSDB;
    private final IPLayer ip;

    public LinkStateRoutingProtocol(IPRouter router) {
        super(router, PROTOCOL_NAME);
        this.ip = router.getIPLayer();
        this.LSDB = new HashMap<IPAddress, LinkStateMessage>();
    }

    @Override
    public void start() throws Exception {
        // Register listener for datagrams with DV routing messages
        ip.addListener(IP_PROTO_LS, this);

        // Register interface attribute listeners to detect metric and status changes
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            iface.addAttrListener(this);
        }

        // building the link state message containing all the neighbours
        LinkStateMessage dvm = new LinkStateMessage();
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface instanceof IPLoopbackAdapter) {
                continue;
            }
            dvm.addLS(iface.getAddress(), iface.getMetric(), iface);
        }
        // init LSDB with own information
        LSDB.put(this.getRouterID(), dvm);

        // sending the state message to each neighbours.
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface instanceof IPLoopbackAdapter) {
                continue;
            }
            iface.send(new Datagram(iface.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, dvm), null);
        }

    }

    @Override
    public void stop() {
        ip.removeListener(IP_PROTO_LS, this);
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            iface.removeAttrListener(this);
        }
    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
        System.out.println(getRouterID() + "received from :"+src.getAddress().toString());
        LinkStateMessage msg = (LinkStateMessage) datagram.getPayload();
        // check if the sequence number is bigger than the one actually stored.
        if (LSDB.get(src.getAddress()) == null || msg.getSequence() > LSDB.get(src.getAddress()).getSequence()) {
            LSDB.put(src.getAddress(), msg);
//            for (LinkStatePacket packet : msg.getPackets()) {
//                ip.addRoute(new LinkStateRoutingEntry(packet.dst, packet.oif, packet));
//            }
            this.SendToAllButSender(src, msg);
        }
        Compute(LSDB);
    }

    private void SendToAllButSender(IPInterfaceAdapter src, LinkStateMessage msg) throws Exception {
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface.getAddress().equals(src.getAddress()) || iface instanceof IPLoopbackAdapter) {
                continue;
            }
            Datagram newDatagram = new Datagram(iface.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, msg);
            iface.send(newDatagram, null);
        }
    }

    private void Compute(Map<IPAddress, LinkStateMessage> LSDB) {
        HashMap<IPAddress, Vertex> vertices = new HashMap<IPAddress, Vertex>();
        for (Map.Entry<IPAddress, LinkStateMessage> entry : LSDB.entrySet()) {
            Vertex newVertex = new Vertex(entry.getKey().toString());
            for (LinkStatePacket packet : entry.getValue().getPackets()) {
                Vertex v = vertices.get(packet.dst);
                if (v == null) {
                    v = new Vertex(packet.dst.toString());
                }
                newVertex.addEdge(new Edge(v, packet.metric));
            }
            vertices.put(entry.getKey(), newVertex);
        }

        for (Vertex v : vertices.values()) {
            //System.out.println("Distance to " + v + ": " + v.minDistance);
            List<Vertex> path = getShortestPathTo(v);
            //System.out.println("Path: " + path);
        }
    }

    @Override
    public void attrChanged(Interface iface, String attr) {
        System.out.println("attribute \"" + attr + "\" changed on interface \"" + iface + "\" : "
                + iface.getAttribute(attr));
    }

    private IPAddress getRouterID() {
        IPAddress routerID = null;
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            IPAddress addr = iface.getAddress();
            if (routerID == null) {
                routerID = addr;
            } else if (routerID.compareTo(addr) < 0) {
                routerID = addr;
            }
        }
        return routerID;
    }
}
