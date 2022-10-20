package com.hao.open.registry;


import com.hao.open.extension.SPI;
import com.hao.open.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

/**
 * 抽象服务查询接口
 */
@SPI
public interface ServiceDiscovery {
    /**
     * 根据 rpcServiceName 查询可用服务地址，要求负载均衡并返回一个
     *
     * @param rpcRequest rpcRequest 对象
     * @return 可用服务的 socket 地址
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
