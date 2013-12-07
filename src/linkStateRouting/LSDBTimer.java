/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package linkStateRouting;

import reso.common.AbstractTimer;
import reso.scheduler.AbstractScheduler;

/**
 *
 * @author alo
 */
public class LSDBTimer extends AbstractTimer{

    private LinkStateRoutingProtocol routingProtocol;
    
    public LSDBTimer(AbstractScheduler scheduler, double interval,LinkStateRoutingProtocol routinProtocol) {
        super(scheduler, interval, false);
        this.routingProtocol = routinProtocol;
    }

    @Override
    protected void run() throws Exception {
        routingProtocol.SendLSP();
    }
    
}
