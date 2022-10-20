package com.hao.open.loadbalance.loadbalancer;


import com.hao.open.loadbalance.AbstractLoadBalance;
import com.hao.open.remoting.dto.RpcRequest;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    private static final int RECYCLE_PERIOD = 6000000;
    private final AtomicInteger current = new AtomicInteger(0);

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        int idx = current.getAndIncrement() % serviceAddresses.size();
        if (current.get() > RECYCLE_PERIOD) {
            current.set(0);
        }
        return serviceAddresses.get(idx);
    }
}
