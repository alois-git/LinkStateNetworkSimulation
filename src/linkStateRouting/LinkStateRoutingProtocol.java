/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linkStateRouting;

import dijkstra.Dijkstra;
import dijkstra.Edge;
import dijkstra.Graph;
import dijkstra.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
    public int LSPDelay;

    // routing table
    public final Map<IPAddress, LinkStateMessage> LSDB;
    // neibourgs table
    public final Map<IPAddress, LinkState> neighborList;

    private final IPLayer ip;
    private AbstractTimer LSDBTimer;

    public LinkStateRoutingProtocol(IPRouter router, int helloDelay, int lspDelay) {
        super(router, PROTOCOL_NAME);
        this.ip = router.getIPLayer();
        this.LSDB = new HashMap<IPAddress, LinkStateMessage>();
        this.neighborList = new HashMap<IPAddress, LinkState>();
        this.HelloDelay = helloDelay;
        this.LSPDelay = lspDelay;
        LSDBTimer = new LSDBTimer(host.getNetwork().getScheduler(), LSPDelay, this);
        LSDBTimer.start();
    }

    @Override
    public void start() throws Exception {
        // Register listener for datagrams with DV routing messages
        ip.addListener(IP_PROTO_LS, this);
        LSDB.put(getRouterID(), new LinkStateMessage(getRouterID()));
        // Register interface attribute listeners to detect metric and status changes
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            iface.addAttrListener(this);
        }

        // saying hello to all his neighbours
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface instanceof IPLoopbackAdapter) {
                continue;
            }
            HelloMessage hello = new HelloMessage(getRouterID(), neighborList.keySet());
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
            // Check if the router is not already in our neighbor list.
            if (!neighborList.containsKey(hello.routerId)) {
                // if the sender has to router id in his neighbor list add him has a neighbor
                // we had it permanently to our neighbor list.
                if (hello.neighborList.contains(getRouterID())) {
                    neighborList.put(hello.routerId, new LinkState(hello.routerId, src.getMetric(), src));
                } else {
                    Map<IPAddress, LinkState> OSPFTemp = neighborList;
                    OSPFTemp.put(hello.routerId, new LinkState(hello.routerId, src.getMetric(), src));
                    HelloMessage helloAnswer = new HelloMessage(getRouterID(), OSPFTemp.keySet());
                    src.send(new Datagram(src.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, helloAnswer), null);
                }
            } else {
                // if the HelloMessage contains a distance which is smaller than the one stored
                // lets replace it.
                if (neighborList.get(hello.routerId).metric > src.getMetric()) {
                    neighborList.put(hello.routerId, new LinkState(hello.routerId, src.getMetric(), src));
                }
            }
        }

        // LinkState message
        if (datagram.getPayload() instanceof LinkStateMessage) {
            LinkStateMessage msg = (LinkStateMessage) datagram.getPayload();

            // Check if it is not present or if sequence number is bigger than the one actually stored.
            if (LSDB.get(msg.routerId) == null || msg.getSequence() > LSDB.get(msg.routerId).getSequence()) {
                LSDB.put(msg.routerId, msg);
                this.SendToAllButSender(src, msg);
            }
            Compute(LSDB);
        }
//
//        System.out.println("LSDB of :" + getRouterID());
//        for (Map.Entry<IPAddress, LinkStateMessage> entry : LSDB.entrySet()) {
//
//            System.out.print("LSP: " + entry.getValue().routerId + ",Seq " + entry.getValue().sequence + ", Nb " + entry.getValue().linkStates.size());
//            System.out.println("");
//            for (LinkState ls : entry.getValue().linkStates) {
//                System.out.println("[" + ls.routerId + ":" + ls.metric + "]");
//            }
//        }
//        System.out.println("");
//        System.out.println("---------------------------");

    }

    public void SendLSP() throws Exception {
        LinkStateMessage LFP = new LinkStateMessage(getRouterID());
        // add neighborList to a LinkStateMessage
        for (LinkState entry : neighborList.values()) {
            LFP.addLS(entry);
        }
        LSDB.put(getRouterID(), LFP);
        for (LinkStateMessage ourNeighbors : LSDB.values()) {
            SendToAll(ourNeighbors);
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

    private void SendToAll(LinkStateMessage msg) throws Exception {
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface instanceof IPLoopbackAdapter) {
                continue;
            }
            Datagram newDatagram = new Datagram(iface.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, msg);
            iface.send(newDatagram, null);
        }
    }

    private void Compute(Map<IPAddress, LinkStateMessage> LSDB) throws Exception {

        HashMap<IPAddress, Node> vertices = new HashMap<IPAddress, Node>();
        ArrayList<Edge> edges = new ArrayList<Edge>();
        for (Map.Entry<IPAddress, LinkStateMessage> entry : LSDB.entrySet()) {
            for (LinkState packet : entry.getValue().getLinkStates()) {
                Node newNode = new Node(packet.routerId.toString());
                vertices.put(packet.routerId, newNode);
            }
        }
        for (Node src : vertices.values()) {
            for (Node dst : vertices.values()) {
                edges.add(new Edge("test", src, dst, Integer.MAX_VALUE));
            }
        }

        for (Map.Entry<IPAddress, LinkStateMessage> entry : LSDB.entrySet()) {
            for (LinkState packet : entry.getValue().getLinkStates()) {
                Node dst = vertices.get(packet.routerId);
                if (dst == null) {
                    vertices.put(packet.routerId, new Node(packet.routerId.toString()));
                    dst = vertices.get(packet.routerId);
                }
                Node src = vertices.get(entry.getKey());
                for (Edge e : edges) {
                    if (e.getSource() == src && e.getDestination() == dst) {
                        e.setWeight(packet.metric);
                    }
                }
            }
        }

        Graph graph = new Graph(vertices.values(), edges);
        Dijkstra dijkstra = new Dijkstra(graph);
        dijkstra.calculate(vertices.get(getRouterID()));

        for (Node routerTo : vertices.values()) {
            System.out.println("from: " + getRouterID() + "to " + routerTo);
            LinkedList<Node> path = dijkstra.getPath(routerTo);
            if (path != null) {
                System.out.println("Cost: " +dijkstra.getDistanceOfPath(path));
                System.out.println("--------------------");
                IPAddress firstInPath = IPAddress.getByAddress(path.get(1).getId());
                IPInterfaceAdapter interfaceTo = neighborList.get(firstInPath).routerInterface;
                LinkState ls = new LinkState(IPAddress.getByAddress(routerTo.getId()), dijkstra.getDistanceOfPath(path), interfaceTo);
                ip.addRoute(new LinkStateRoutingEntry(ls.routerId, ls.routerInterface, ls));
            }
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
