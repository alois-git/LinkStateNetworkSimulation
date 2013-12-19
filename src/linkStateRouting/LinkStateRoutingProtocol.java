/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linkStateRouting;

import dijkstra.Dijkstra;
import dijkstra.Edge;
import dijkstra.FibonacciHeapNode;
import dijkstra.Graph;
import dijkstra.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private LSPTimer LSPTimer;
    private HelloTimer HelloTimer;

    public LinkStateRoutingProtocol(IPRouter router, int helloDelay, int lspDelay) {
        super(router, PROTOCOL_NAME);
        this.ip = router.getIPLayer();
        this.LSDB = new HashMap<IPAddress, LinkStateMessage>();
        this.neighborList = new HashMap<IPAddress, LinkState>();
        this.HelloDelay = helloDelay;
        this.LSPDelay = lspDelay;
        LSPTimer = new LSPTimer(host.getNetwork().getScheduler(), LSPDelay, true, this);
        HelloTimer = new HelloTimer(host.getNetwork().getScheduler(), HelloDelay, true, this);
        LSPTimer.start();
        HelloTimer.start();
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
        SendHelloToNeighbors();
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
            LinkStateMessage currentNeighbors = new LinkStateMessage(getRouterID());
            for (LinkState ls : neighborList.values()) {
                currentNeighbors.addLS(ls);
            }
            LSDB.put(getRouterID(), currentNeighbors);
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

    @Override
    public void attrChanged(Interface iface, String attr) {
        if (attr.equals("metric")) {
            for (Entry<IPAddress, LinkState> ls : neighborList.entrySet()) {
                if (ls.getValue().routerInterface.equals(iface)) {
                    neighborList.put(ls.getKey(), new LinkState(ls.getValue().routerId, (int) iface.getAttribute("metric"), ls.getValue().routerInterface));
                }
            }
        }
        if (attr.equals("state")) {
            for (Entry<IPAddress, LinkState> ls : neighborList.entrySet()) {
                if (ls.getValue().routerInterface.equals(iface)) {
                    if (iface.isActive()) {
                        neighborList.put(ls.getKey(), new LinkState(ls.getValue().routerId, (int) iface.getAttribute("metric"), ls.getValue().routerInterface));
                    } else {
                        neighborList.put(ls.getKey(), new LinkState(ls.getValue().routerId, Integer.MAX_VALUE, ls.getValue().routerInterface));
                    }
                }
            }
        }
        try {
            SendLSPToNeighbors();
        } catch (Exception ex) {
            Logger.getLogger(LinkStateRoutingProtocol.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void SendHelloToNeighbors() throws Exception {
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface instanceof IPLoopbackAdapter) {
                continue;
            }
            HelloMessage hello = new HelloMessage(getRouterID(), neighborList.keySet());
            iface.send(new Datagram(iface.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, hello), null);
        }
    }

    public void SendLSPToNeighbors() throws Exception {
        for (IPInterfaceAdapter iface : ip.getInterfaces()) {
            if (iface instanceof IPLoopbackAdapter) {
                continue;
            }
            LinkStateMessage LSM = new LinkStateMessage(getRouterID());
            for (LinkState ls : neighborList.values()) {
                LSM.addLS(ls);
            }
            iface.send(new Datagram(iface.getAddress(), IPAddress.BROADCAST, IP_PROTO_LS, 1, LSM), null);
        }
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

        HashMap<IPAddress, FibonacciHeapNode> vertices = new HashMap<IPAddress, FibonacciHeapNode>();
        ArrayList<Edge> edges = new ArrayList<Edge>();

        // Construct the nodes list from the LSDB entries.
        for (Map.Entry<IPAddress, LinkStateMessage> entry : LSDB.entrySet()) {
            FibonacciHeapNode newNode = new FibonacciHeapNode(entry.getKey());
            vertices.put(entry.getKey(), newNode);
        }
        // Populate all the possible edge to MAX value (infinity).
        for (FibonacciHeapNode src : vertices.values()) {
            for (FibonacciHeapNode dst : vertices.values()) {
                edges.add(new Edge("", src, dst, Integer.MAX_VALUE));
            }
        }

        // Put the real distance to existing node.
        // Take all the LSDB entry and build the edges.
        for (Map.Entry<IPAddress, LinkStateMessage> entry : LSDB.entrySet()) {
            for (LinkState packet : entry.getValue().getLinkStates()) {
                FibonacciHeapNode dst = vertices.get(packet.routerId);
                if (dst == null) {
                    vertices.put(packet.routerId, new FibonacciHeapNode(packet.routerId));
                    dst = vertices.get(packet.routerId);
                }
                FibonacciHeapNode src = vertices.get(entry.getKey());
                for (Edge e : edges) {
                    if (e.getSource() == src && e.getDestination() == dst) {
                        e.setWeight(packet.metric);
                    }
                }
            }
        }

        // Build the graph and calculate shortest paths for the router.
        Graph graph = new Graph(vertices.values(), edges);
        Dijkstra dijkstra = new Dijkstra(graph);
        dijkstra.calculate(vertices.get(getRouterID()));

        // add the route entry to the FIB table.
        for (FibonacciHeapNode routerTo : vertices.values()) {
            LinkedList<FibonacciHeapNode> path = dijkstra.getPath(routerTo);
            if (path != null) {
                IPAddress firstInPath = path.get(1).getData();
                IPInterfaceAdapter interfaceTo = neighborList.get(firstInPath).routerInterface;
                LinkState ls = new LinkState(routerTo.getData(), dijkstra.getDistanceOfPath(path), interfaceTo);
                ip.addRoute(new LinkStateRoutingEntry(ls.routerId, ls.routerInterface, ls));
            }
        }
    }

}
