/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linkStateRouting;

import java.util.ArrayList;
import java.util.List;
import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;

/**
 *
 * @author alo
 */
public class LinkStateMessage {

    public static List<LinkStatePacket> packets;

    public LinkStateMessage() {
        packets = new ArrayList<LinkStatePacket>();
    }

    public void addLS(IPAddress dst, int routerId, int sequence,ArrayList<LinkStatePacket.Neighbor> neighbors,IPInterfaceAdapter oif) {
        packets.add(new LinkStatePacket(dst, routerId, sequence,neighbors,oif));
    }

    public String toString() {
        String s = "";
        for (LinkStatePacket packet : packets) {
            s += " " + packet;
        }
        return s;
    }
}
