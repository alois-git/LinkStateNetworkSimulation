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
public class LinkState {

    public final IPAddress dst;
    public final int metric;
    public final IPInterfaceAdapter oif;

    public LinkState(IPAddress dst, int metric, IPInterfaceAdapter oif) {
        this.oif = oif;
        this.dst = dst;
        this.metric = metric;
    }

    public String toString() {
        return "LS[" + dst + "," + ((metric == Integer.MAX_VALUE) ? "inf" : metric) + "," + oif + "]";
    }
}
