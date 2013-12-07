/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linkStateRouting;

import java.util.List;
import reso.common.Message;
import reso.ip.IPAddress;

/**
 *
 * @author alo
 */
public class HelloMessage implements Message {

    public final IPAddress routerId;
    public final List<IPAddress> neighborList;
    public final int neighborNumber;

    public HelloMessage(IPAddress routerId, List<IPAddress> neighborList) {
        this.routerId = routerId;
        this.neighborList = neighborList;
        this.neighborNumber = neighborList.size();
    }
}
