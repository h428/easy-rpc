package com.hao.open.provider;

import com.hao.open.config.RpcServiceConfig;

/**
 * 存储和提供服务对象，对注册中心的进一步封装
 */
public interface ServiceProvider {

    /**
     * 获取一个已发布的服务
     *
     * @param rpcServiceName rpc service name
     * @return service object
     */
    Object getService(String rpcServiceName);

    /**
     * 发布一个服务
     *
     * @param rpcServiceConfig 服务配置
     */
    void publishService(RpcServiceConfig rpcServiceConfig);
}
