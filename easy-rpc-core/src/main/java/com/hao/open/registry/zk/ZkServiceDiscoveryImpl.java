package com.hao.open.registry.zk;

import com.hao.open.enums.RpcErrorMessageEnum;
import com.hao.open.exception.RpcException;
import com.hao.open.extension.ExtensionLoader;
import com.hao.open.loadbalance.LoadBalance;
import com.hao.open.registry.ServiceDiscovery;
import com.hao.open.registry.zk.util.CuratorUtils;
import com.hao.open.remoting.dto.RpcRequest;
import com.hao.open.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 采用 zookeeper 做注册中心的服务查询实现
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    // 负载均衡
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        // 基于 SPI 读取负载均衡策略
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        // 确认本次 rpc 调用的接口标志
        String rpcServiceName = rpcRequest.getRpcServiceName();
        //
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // load balancing
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }
}
