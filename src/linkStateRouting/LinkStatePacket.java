/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linkStateRouting;

import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;

/**
 *
 * @author alo
 */
public class LinkStatePacket {

    public final IPAddress dst;
    public final int metric;
    public final IPInterfaceAdapter oif;

    public LinkStatePacket(IPAddress dst, int metric,  IPInterfaceAdapter oif) {
        this.oif = oif;
        this.dst = dst;
        this.metric = metric;
    }
}
