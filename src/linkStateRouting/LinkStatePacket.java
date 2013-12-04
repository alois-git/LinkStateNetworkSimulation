/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linkStateRouting;

import java.util.ArrayList;
import java.util.List;
import reso.common.Message;
import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;

/**
 *
 * @author alo
 */
public class LinkStatePacket {

    public class Neighbor {

        public int id;
        public int cost;
    }

    public final IPAddress dst;
    public final int routerId;
    public final int sequenceNumber;
    public final int neighborsNumber;
    public final List<Neighbor> neighbors;
    public final IPInterfaceAdapter oif;

    public LinkStatePacket(IPAddress dst, int id, int sequence, ArrayList<Neighbor> neighbors, IPInterfaceAdapter oif) {
        this.oif = oif;
        this.dst = dst;
        this.routerId = id;
        this.sequenceNumber = sequence;
        this.neighbors = neighbors;
        this.neighborsNumber = neighbors.size();
    }

    public String toString() {
        String s = "";
        for (Neighbor nb : neighbors) {
            s += " " + nb;
        }
        return s;
    }
}
