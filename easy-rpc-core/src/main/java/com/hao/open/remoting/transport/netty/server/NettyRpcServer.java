package com.hao.open.remoting.transport.netty.server;

import com.hao.open.config.BaseConfig;
import com.hao.open.config.CustomShutdownHook;
import com.hao.open.config.RpcServiceConfig;
import com.hao.open.factory.SingletonFactory;
import com.hao.open.provider.ServiceProvider;
import com.hao.open.provider.impl.ZkServiceProviderImpl;
import com.hao.open.remoting.transport.netty.codec.RpcMessageDecoder;
import com.hao.open.remoting.transport.netty.codec.RpcMessageEncoder;
import com.hao.open.utils.RuntimeUtil;
import com.hao.open.utils.concurrent.pool.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Netty 服务端
 */
@Slf4j
public class NettyRpcServer {

    // 采用 zookeeper 做注册中心
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    /**
     * 向注册中心注册服务
     *
     * @param rpcServiceConfig 服务配置
     */
    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    @SneakyThrows
    public void start() {
        // 清理工作
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress();
        // bossGroup 负责处理 accept 事件，线程数一般设置为 1
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // workerGroup 负责处理通用的 IO 读写事件，一般不设置线程数
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        // 额外线程池处理耗时的业务逻辑，避免阻塞 IO 线程影响吞吐量
        // You should never block on netty worker threads, especially for network I/O.
        // that is the entire point of using a non-blocking asynchronous framework like netty.
        DefaultEventLoopGroup serviceHandlerGroup = new DefaultEventLoopGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false));
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    // 设置 bossGroup 和 workerGroup
                    .group(bossGroup, workerGroup)
                    // 设置 SSC 通道
                    .channel(NioServerSocketChannel.class)
                    // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // TCP 默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输，在数据不足时可能会等到足够数据再发送
                    // 因此会造成延时，TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法，设置为 true 即关闭延时
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // ChannelOption.SO_KEEPALIVE 参数对应于套接字选项中的SO_KEEPALIVE，用于设置是否开启 TCP 底层心跳机制
                    // 当设置该选项以后，连接会测试链接的状态，这个选项用于可能长时间没有数据交流的连接
                    // 当设置该选项以后，如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文。
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // SSC 通道添加日志处理器
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // SC 通道初始化
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            // IdleStateHandler 为 Netty 提供的心跳机制处理器，一般会结合 userEventTriggered 使用
                            // 下行代码设置 30s 内如果没有收到 channel 的数据，会触发一个 IdleState#READER_IDLE 事件
                            // 我们会要求客户端 30s 内发送一个心跳，同时一般在处理该事件的 Handler 中，我们会关闭当前 channel
                            // 本例中处理 IdleState.READER_IDLE 的代码在 NettyRpcServerHandler.userEventTriggered() 中
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            // 消息出站编码器，将 Message 转化为 ByteBuf
                            p.addLast(new RpcMessageEncoder());
                            // 消息入站解码器，将 ByteBuf 解析为 RpcMessage
                            // 该处理器同时继承了 LengthFieldBasedFrameDecoder，用于处理半包粘包问题
                            p.addLast(new RpcMessageDecoder());
                            // 处理 RpcMessage，转化为对本地 service 的调用并返回结果
                            // 注意以 serviceHandlerGroup 运行，避免注册 worker 线程
                            p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 绑定端口，并得到 ChannelFuture
            ChannelFuture future = bootstrap.bind(host, BaseConfig.NETTY_PORT);
            // 获取 channel，并阻塞住直到关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("occur exception when start server:", e);
        } finally {
            // 优雅关闭各个线程池
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
