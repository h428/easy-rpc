package com.hao.open.registry;

import com.hao.open.extension.SPI;

import java.net.InetSocketAddress;

/**
 * 服务发布抽象接口，不同注册中心有不同实现
 */
@SPI
public interface ServiceRegistry {

    /**
     * 注册服务
     *
     * @param rpcServiceName    服务提供者名称
     * @param inetSocketAddress 服务提供者服务 socket 地址
     */
    void registryService(String rpcServiceName, InetSocketAddress inetSocketAddress);
}
