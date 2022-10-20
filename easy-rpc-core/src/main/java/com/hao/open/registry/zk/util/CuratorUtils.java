package com.hao.open.registry.zk.util;

import com.hao.open.enums.RpcConfigEnum;
import com.hao.open.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Curator 工具类，封装 zookeeper 基本操作用于服务发布和服务查询
 * 采用 zookeeper 做注册中心时，“服务接口-服务提供者”在 zookeeper 中的数据结构如下（模拟 dubbo）：
 * 1. 首先统一约定一个前缀作为祖先节点，我们采用 /easy-rpc（对应 dubbo 中的 /dubbo）
 * 2. 基于接口发布服务，故每个服务接口会在 /easy-rpc 下创建一个节点作为各个服务提供者实例的父节点
 * 3. 每个服务提供者会将自己的 ip 作为服务接口的子节点，而且是一个临时节点，在客户端断开后一段时间会自动移除
 * 例如 DemoService 有三个服务提供者，则会创建下列临时节点：
 * /easy-rpc/com.hao.demo.DemoService/192.168.25.41
 * /easy-rpc/com.hao.demo.DemoService/192.168.25.42
 * /easy-rpc/com.hao.demo.DemoService/192.168.25.43
 */
@Slf4j
public class CuratorUtils {

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;
    private static CuratorFramework zkClient;
    public static final String ZK_REGISTER_ROOT_PATH = "/easy-rpc";
    public static final String DEFAULT_ZOOKEEPER_ADDRESS = "localhost:2181";
    /**
     * 已经注册到 zookeeper 的服务提供者列表
     * 举例接口：/easy-rpc/com.hao.open.service.UserService/192.168.25.41
     */
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    /**
     * 某个服务接口对应的服务提供者列表的缓存 map
     * key: 服务接口，例如 /easy-rpc/com.hao.open.service.UserService
     * values: 服务接口对应的服务提供者 ip 列表，各个 ip 会作为临时子节点存在，可能有多个服务提供者故是一个 List
     */
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();

    private CuratorUtils() {

    }

    /**
     * 创建临时节点
     *
     * @param zkClient        客户端
     * @param rpcInstancePath 节点路径
     */
    public static void createEphemeralNode(CuratorFramework zkClient, String rpcInstancePath) {
        try {
            // 已注册过的节点跳过注册
            if (REGISTERED_PATH_SET.contains(rpcInstancePath) || zkClient.checkExists().forPath(rpcInstancePath) != null) {
                log.info("节点 {} 已存在，无需重复注册，本次注册跳过", rpcInstancePath);
            } else {
                // 注册 rpcInstancePath 节点并添加到缓存集合
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(rpcInstancePath);
                log.info("创建临时节点 {} 成功", rpcInstancePath);
            }
            // 添加到缓存
            REGISTERED_PATH_SET.add(rpcInstancePath);
        } catch (Exception e) {
            log.error("创建临时节点 {} 发生异常：{}", rpcInstancePath, e);
        }
    }

    /**
     * 查询子节点列表，对应业务为：根据服务接口查询服务提供者实例列表
     *
     * @param zkClient       客户端
     * @param rpcServiceName 服务接口的 path
     * @return 子节点列表
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        // 缓存命中，直接返回缓存
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }

        // 未命中则向 zookeeper 查询
        try {
            String rpcServicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
            List<String> serviceIpList = zkClient.getChildren().forPath(rpcServicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceIpList);
            // 添加 watcher，以在服务列表发生变化时刷新缓存
            registerWatcher(zkClient, rpcServiceName);
            return serviceIpList;
        } catch (Exception e) {
            log.error("查询 {} 的子节点时发生异常：{}", rpcServiceName, e);
            return null;
        }
    }

    /**
     * 用于在退出以及重启当前应用时，清空当前应用注册的所有临时节点（不同服务接口）
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("移除节点 {} 时发生异常：{}", p, e);
            }
        });
    }

    public static CuratorFramework getZkClient() {
        // check if user has set zk address
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;
        // if zkClient has been started, return directly
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                // the server to connect to (can be a server list)
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        try {
            // wait 30s until connect to the zookeeper
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    /**
     * 为服务接口节点添加 watcher，但子节点列表（即服务提供者列表）发生变化时触发
     *
     * @param zkClient       客户端
     * @param rpcServiceName 服务接口
     * @throws Exception 监听异常
     */
    private static void registerWatcher(CuratorFramework zkClient, String rpcServiceName) throws Exception {
        String rpcServicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, rpcServicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = (client, event) -> {
            List<String> serviceIpList = client.getChildren().forPath(rpcServicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceIpList);
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

}
