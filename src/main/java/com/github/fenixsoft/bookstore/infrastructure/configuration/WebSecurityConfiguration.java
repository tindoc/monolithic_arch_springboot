package com.github.fenixsoft.bookstore.infrastructure.configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Spring Security 安全配置
 * <p>
 * 移除静态资源目录的安全控制，避免 Spring Security 默认禁止 HTTP 缓存的行为
 *
 * @author icyfenix@gmail.com
 * @date 2020/4/8 0:09
 **/
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter { // todo WebSecurityConfigurerAdapter 和 WebSecurityConfigurer 的区别？AuthenticationServerConfiguration 类使用第二个

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().cacheControl().disable();
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/static/**");
    }
}
