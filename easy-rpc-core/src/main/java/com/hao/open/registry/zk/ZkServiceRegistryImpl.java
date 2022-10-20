package com.hao.open.registry.zk;

import com.hao.open.registry.ServiceRegistry;
import com.hao.open.registry.zk.util.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

/**
 * 使用 zookeeper 做注册中心的服务发布实现
 */
public class ZkServiceRegistryImpl implements ServiceRegistry {

    @Override
    public void registryService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        // 当前客户端实例对应 path
        String rpcInstancePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        // 注册指定接口以及当前实例到注册中心
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createEphemeralNode(zkClient, rpcInstancePath);
    }
}
