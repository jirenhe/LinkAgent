/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.lettuce.interceptor.spring;

import com.pamirs.attach.plugin.common.datasource.redisserver.RedisServerMatchStrategy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.*;


/**
 * @Auther: vernon
 * @Date: 2021/9/10 12:15
 * @Description:
 */

public class LettuceFactoryProxy {

    private LettuceConnectionFactory biz;
    private LettuceConnectionFactory pressure;

    public static LettuceFactoryProxy INSTANCE = new LettuceFactoryProxy();

    public LettuceFactoryProxy setBiz(LettuceConnectionFactory factory) {
        if (null == biz) {
            this.biz = factory;
        }
        return this;
    }

    LettuceConnectionFactory getFactory() {
        if (!Pradar.isClusterTest()) {
            return biz;
        }
        if (pressure == null) {
            synchronized (this) {
                if (pressure == null) {
                    pressure = new FactoryInitializer(biz).create();
                }
            }
        }
        return pressure;
    }
}

class FactoryInitializer extends MatchStrategy {
    LettuceConnectionFactory biz;

    FactoryInitializer(LettuceConnectionFactory biz) {
        this.biz = biz;
    }

    LettuceConnectionFactory create() {

        RedisServerMatchStrategy matcher = null;

        if (biz == null) {
            return null;
        }


        RedisConfiguration standaloneConfiguration = biz.getStandaloneConfiguration();
        RedisConfiguration clusterConfiguration = biz.getClusterConfiguration();
        RedisConfiguration sentinelConfiguration = biz.getSentinelConfiguration();

        if (clusterConfiguration != null) {
            matcher = CLUSTER_MODE_MATCHER;
            Set nodes = ((RedisClusterConfiguration) clusterConfiguration).getClusterNodes();
            String password = null;
            try {
                char[] passwdbyte = ((RedisClusterConfiguration) clusterConfiguration).getPassword().get();
                password = new String(passwdbyte);

                ShadowRedisConfig shadowRedisConfig = matcher.getConfig(Arrays.asList(nodes));

                String shadowPassword = shadowRedisConfig.getPassword();
                shadowRedisConfig.getNodes();
                shadowRedisConfig.getDatabase();
                shadowRedisConfig.getMaster();
                RedisClusterConfiguration shadowRedisClusterConfiguration
                        = new RedisClusterConfiguration();
            } catch (NoSuchElementException e) {
                //
            }

        } else if (sentinelConfiguration != null) {
            RedisSentinelConfiguration redisSentinelConfiguration = (RedisSentinelConfiguration) sentinelConfiguration;
            String masterName = redisSentinelConfiguration.getMaster().getName();
            String password = null;
            try {
                char[] passwdbyte = redisSentinelConfiguration.getPassword().get();
                password = new String(passwdbyte);
            } catch (NoSuchElementException e) {
                //
            }
            Integer database = Integer.parseInt(String.valueOf(redisSentinelConfiguration.getDatabase()));
            Set<RedisNode> nodes = redisSentinelConfiguration.getSentinels();


        }
        /**
         *standalone肯定不为空 放后面 因为可能为localhost
         */
        else if (standaloneConfiguration != null &&
                !("localhost".equals(((RedisStandaloneConfiguration) standaloneConfiguration).getHostName()))) {
            matcher = SINGLE_MODE_MATCHER;
            RedisStandaloneConfiguration configuration = (RedisStandaloneConfiguration) standaloneConfiguration;

            String host = configuration.getHostName();
            Integer port = configuration.getPort();
            Integer db = configuration.getDatabase();
            String password = null;
            try {
                char[] passwdbyte = configuration.getPassword().get();
                password = new String(passwdbyte);
            } catch (NoSuchElementException e) {
                //
            }
            ShadowRedisConfig shadowRedisConfig = matcher.getConfig(new Key(host, port, db, password));
            if (shadowRedisConfig == null) {
                return null;
            } else {

                String[] spilter = shadowRedisConfig.getNodes().split(":");
                String shadowHost = spilter[0];
                Integer shadowPort = Integer.valueOf(spilter[1]);
                Integer shadowDb = shadowRedisConfig.getDatabase();
                String shadowPassword = shadowRedisConfig.getPassword();

                LettuceConnectionFactory shadowConnectionFactory = new LettuceConnectionFactory();
                shadowConnectionFactory.setHostName(shadowHost);
                shadowConnectionFactory.setPort(shadowPort);
                if (shadowPassword != null) {
                    shadowConnectionFactory.setPassword(shadowPassword);
                }
                if (shadowDb != null) {
                    shadowConnectionFactory.setDatabase(shadowDb);
                } else {
                    shadowConnectionFactory.setDatabase(biz.getDatabase());
                }
                extraProperties(biz, shadowConnectionFactory);

                //初始化
                shadowConnectionFactory.afterPropertiesSet();
                /*    DefaultListableBeanFactory defaultListableBeanFactory = PradarSpringUtil.getBeanFactory();*/
                return shadowConnectionFactory;

            }

        }
        return null;

    }

    private void extraProperties(LettuceConnectionFactory biz, LettuceConnectionFactory pressure) {
        pressure.setTimeout(biz.getTimeout());
        pressure.setShareNativeConnection(biz.getShareNativeConnection());
        Reflect.on(pressure).set("clientConfiguration", biz.getClientConfiguration());
    }
}

class Key {
    String host;
    Integer port;
    Integer db;
    String password;

    public Key(String host, Integer port, Integer db, String password) {
        this.host = host;
        this.port = port;
        this.db = db;
        this.password = password;
    }
}
