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
import reso.common.AbstractTimer;
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
    public int HelloDelay;

    public final Map<IPAddress, LinkStateMessage> LSDB;
    // neibourgs table
    public final List<IPAddress> neighborList;
    private final IPLayer ip;

    public LinkStateRoutingProtocol(IPRouter router, int helloDelay) {
        super(router, PROTOCOL_NAME);
        this.ip = router.getIPLayer();
        this.LSDB = new HashMap<IPAddress, LinkStateMessage>();
        this.neighborList = new ArrayList<>();
        this.HelloDelay = helloDelay;
    }

    @Override
    public void start() throws Exception {
        // Register listener for datagrams with DV routing messages
        ip.addListener(IP_PROTO_LS, this);

        // Register interface attribute listeners to detect metric and status changes
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            iface.addAttrListener(this);
        }

        // saying hello to all his neighbours
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface instanceof IPLoopbackAdapter) {
                continue;
            }
            HelloMessage hello = new HelloMessage(getRouterID(), neighborList);
            iface.send(new Datagram(iface.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, hello), null);
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

        // hello message received from one of our neibourg
        if (datagram.getPayload() instanceof HelloMessage) {
            HelloMessage hello = (HelloMessage) datagram.getPayload();

            if (!neighborList.contains(hello.routerId)) {

                // if the sender has to router id in his neighbor list add him has a neighbor
                // we had it permanently to our neighbor list.
                if (hello.neighborList.contains(getRouterID())) {
                    neighborList.add(hello.routerId);
                } else {
                    List<IPAddress> OSPFTemp = neighborList;
                    OSPFTemp.add(hello.routerId);
                    HelloMessage helloAnswer = new HelloMessage(getRouterID(), OSPFTemp);
                    src.send(new Datagram(src.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, helloAnswer), null);
                }

            }

        }
        System.out.println("Neighbor of :" + getRouterID());
        for (IPAddress routerId : neighborList) {
            System.out.println(routerId);
        }
        System.out.println("-------------------------");

        // LinkState message
        if (datagram.getPayload() instanceof LinkStateMessage) {
            LinkStateMessage msg = (LinkStateMessage) datagram.getPayload();

            // Check if it is not present or if sequence number is bigger than the one actually stored.
            if (LSDB.get(src.getAddress()) == null || msg.getSequence() > LSDB.get(src.getAddress()).getSequence()) {
                LSDB.put(src.getAddress(), msg);
                // browse every link from a neibourg
                for (LinkState ls : msg.getLinkStates()) {
                    if (ip.hasAddress(ls.routerId)) {
                        continue;
                    }
                    LinkState newLS = new LinkState(ls.routerId, addMetric(ls.metric, src.getMetric()), src);
                    ip.addRoute(new LinkStateRoutingEntry(newLS.routerId, newLS.oif, newLS));
                }
                this.SendToAllButSender(src, msg);
            }
            Compute(LSDB);
        }

    }

    public int addMetric(int m1, int m2) {
        if (((long) m1) + ((long) m2) > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return m1 + m2;
    }

    private void SendToAllButSender(IPInterfaceAdapter src, LinkStateMessage msg) throws Exception {
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface.equals(src) || iface instanceof IPLoopbackAdapter) {
                continue;
            }
            Datagram newDatagram = new Datagram(iface.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, msg);
            iface.send(newDatagram, null);
        }
    }

    private void Compute(Map<IPAddress, LinkStateMessage> LSDB) throws Exception {
        HashMap<IPAddress, Vertex> vertices = new HashMap<IPAddress, Vertex>();
        for (Map.Entry<IPAddress, LinkStateMessage> entry : LSDB.entrySet()) {
            Vertex newVertex = new Vertex(entry.getKey().toString());
            for (LinkState packet : entry.getValue().getLinkStates()) {
                Vertex v = vertices.get(packet.routerId);
                if (v == null) {
                    v = new Vertex(packet.routerId.toString());
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
