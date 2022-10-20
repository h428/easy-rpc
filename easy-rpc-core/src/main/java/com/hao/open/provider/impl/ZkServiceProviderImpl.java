package com.hao.open.provider.impl;

import com.hao.open.config.BaseConfig;
import com.hao.open.config.RpcServiceConfig;
import com.hao.open.enums.RpcErrorMessageEnum;
import com.hao.open.exception.RpcException;
import com.hao.open.extension.ExtensionLoader;
import com.hao.open.provider.ServiceProvider;
import com.hao.open.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    /**
     * service 实例的缓存 map
     * key: rpc service name (interface name + version + group)
     * value: service 实例
     */
    private final Map<String, Object> serviceMap;
    /**
     * 已注册的服务
     */
    private final Set<String> registeredService;

    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            // 缓存
            this.addService(rpcServiceConfig);
            // 真正注册
            this.serviceRegistry.registryService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, BaseConfig.NETTY_PORT));
        } catch (UnknownHostException e) {
            log.error("发布服务发生异常，", e);
        }
    }

    private void addService(RpcServiceConfig rpcServiceConfig) {
        // 获取服务的注册标识符，一般是“接口+组+版本”
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("成功添加服务 {}, 对应接口列表为 {}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }
}
