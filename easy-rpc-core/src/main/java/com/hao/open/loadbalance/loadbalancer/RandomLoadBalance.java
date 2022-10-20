package com.hao.open.loadbalance.loadbalancer;


import com.hao.open.loadbalance.AbstractLoadBalance;
import com.hao.open.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * 随机算法
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    private final Random random = new Random();

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
