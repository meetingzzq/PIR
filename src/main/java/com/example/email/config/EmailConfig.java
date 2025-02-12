package com.example.email.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.mail.*;
import java.util.Properties;

@Configuration
public class EmailConfig {
    @Value("${email.host}")
    private String host;

    @Value("${email.port}")
    private String port;

    @Value("${email.username}")
    private String username;

    @Value("${email.password}")
    private String password;

    @Value("${email.protocol}")
    private String protocol;

    @Bean
    public Session emailSession() {
        Properties properties = new Properties();
        // 基本配置
        properties.setProperty("mail.store.protocol", protocol);
        properties.setProperty("mail.imaps.host", host);
        properties.setProperty("mail.imaps.port", port);

        // 安全配置
        properties.setProperty("mail.imaps.ssl.enable", "true");
        properties.setProperty("mail.imaps.ssl.trust", "*");
        properties.setProperty("mail.imaps.ssl.protocols", "TLSv1.2");

        // 禁用 STARTTLS
        properties.setProperty("mail.imaps.starttls.enable", "false");
        properties.setProperty("mail.imaps.starttls.required", "false");

        // Socket 工厂配置
        properties.setProperty("mail.imaps.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.imaps.socketFactory.fallback", "false");
        properties.setProperty("mail.imaps.socketFactory.port", port);

        // 认证配置
        properties.setProperty("mail.imaps.auth", "true");
        properties.setProperty("mail.imaps.auth.login.disable", "true");
        properties.setProperty("mail.imaps.auth.plain.disable", "false");

        // 修改连接池配置，关闭调试
        properties.setProperty("mail.imaps.connectionpool.debug", "false");
        properties.setProperty("mail.imaps.connectionpoolsize", "5");

        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
}