package com.liteisle.common.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "spring.data.redis")
@Component
public class RedisProps {
    private String host;
    private Integer port;
    private String password;

    public String getAddress() {
        return "redis://" + host + ":" + port;
    }

}
