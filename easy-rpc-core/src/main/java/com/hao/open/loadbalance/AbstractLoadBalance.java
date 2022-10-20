package com.hao.open.loadbalance;


import com.hao.open.remoting.dto.RpcRequest;
import com.hao.open.utils.CollectionUtil;

import java.util.List;

/**
 * 负载均衡抽象实现，封装统一逻辑
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        if (CollectionUtil.isEmpty(serviceAddresses)) {
            return null;
        }
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        return doSelect(serviceAddresses, rpcRequest);
    }

    /**
     * 负载均衡逻辑接口，不同负载均衡算法有不同具体实现
     *
     * @param serviceAddresses 可用服务列表
     * @param rpcRequest       rpc 请求
     * @return 返回选择的地址
     */
    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);

}
