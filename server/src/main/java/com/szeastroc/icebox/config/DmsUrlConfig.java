package com.szeastroc.icebox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/5/24 16:25
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "dms")
public class DmsUrlConfig {
    private String toDmsUrl;
}
