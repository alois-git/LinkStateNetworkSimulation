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
public class LinkStateMessage implements Message{

    public List<LinkStatePacket> packets;
    public static int sequence = 0;

    public int getSequence() {
        return sequence;
    }

    public List<LinkStatePacket> getPackets() {
        return packets;
    }

    public LinkStateMessage() {
        packets = new ArrayList<LinkStatePacket>();
        sequence++;
    }

    public void addLS(IPAddress dst, int metric, IPInterfaceAdapter oif) {
        packets.add(new LinkStatePacket(dst, metric, oif));
    }

    public String toString() {
        String s = "";
        for (LinkStatePacket packet : packets) {
            s += " " + packet;
        }
        return s;
    }
}
