package com.hao.open.loadbalance;


import com.hao.open.extension.SPI;
import com.hao.open.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡抽象接口
 */
@SPI
public interface LoadBalance {
    /**
     * 根据传入的服务列表和 rpc 请求进行负载均衡
     *
     * @param serviceUrlList 可用服务列表
     * @param rpcRequest     rpc 请求
     * @return target service address
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
