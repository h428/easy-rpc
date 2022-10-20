package com.hao.open.config;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcServiceConfig {

    private String version = "";

    private String group = "";

    private Object service;

    /**
     * 获取服务的标识，包括“接口包全名 + group + version”
     *
     * @return rpcServiceName
     */
    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }

}
