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
public class LinkStateMessage implements Message {

    public IPAddress routerId;
    public List<LinkState> linkStates;
    public static int sequence = 0;
    
    public LinkStateMessage(IPAddress routerId){
        this.routerId = routerId;
    }

    public int getSequence() {
        return sequence;
    }

    public List<LinkState> getLinkStates() {
        return linkStates;
    }

    public LinkStateMessage() {
        linkStates = new ArrayList<LinkState>();
        sequence++;
    }

    public void addLS(IPAddress routerId, int metric, IPInterfaceAdapter oif) {
        linkStates.add(new LinkState(routerId, metric, oif));
    }

    public String toString() {
        String s = "";
        for (LinkState packet : linkStates) {
            s += " " + packet;
        }
        return s;
    }
}
