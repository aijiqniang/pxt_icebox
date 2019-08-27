package com.szeastroc.icebox.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Tulane
 * 2019/4/28
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "xcx")
public class XcxConfig {

    private String appid;

}
