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
package com.shulie.instrument.simulator.agent.core.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;

import com.shulie.instrument.simulator.agent.core.util.AddressUtils;
import com.shulie.instrument.simulator.agent.core.util.ConfigUtils;
import com.shulie.instrument.simulator.agent.core.util.PidUtils;
import com.shulie.instrument.simulator.agent.core.util.PropertyPlaceholderHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * agent 配置
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/17 8:09 下午
 */
public class CoreConfig {
    private final Logger logger = LoggerFactory.getLogger(CoreConfig.class);

    private final static String CONFIG_PATH_NAME = "config";
    private final static String AGENT_PATH_NAME = "agent";
    private final static String PROVIDER_PATH_NAME = "provider";
    private final static String LOG_PATH_NAME = "simulator.log.path";
    private final static String LOG_LEVEL_NAME = "simulator.log.level";
    private final static String MULTI_APP_SWITCH = "simulator.multiapp.switch.on";
    private final static String DEFAULT_LOG_LEVEL = "info";

    private static final String RESULT_FILE_PATH = System.getProperties().getProperty("user.home")
        + File.separator + "%s" + File.separator + ".simulator.token";
    /**
     * 存放所有的 agent 配置
     */
    private final Map<String, String> configs = new HashMap<String, String>();

    /**
     * agent 配置文件读取的配置
     */
    private final Map<String, String> agentFileConfigs = new HashMap<String, String>();

    /**
     * agent home 路径
     */
    private final String agentHome;

    /**
     * config 文件路径
     */
    private final String configFilePath;

    /**
     * spi 目录路径
     */
    private final String providerFilePath;

    /**
     * simulator 目录路径
     */
    private final String simulatorHome;

    /**
     * simulator 启动jar 路径
     */
    private final String simulatorJarPath;

    /**
     * log 配置文件路径
     */
    private final String logConfigFilePath;

    /**
     * attach的进程 id
     */
    private long attachId = -1L;

    /**
     * attach 的进程名称
     */
    private String attachName;

    private ScheduledExecutorService service;

    public CoreConfig(String agentHome) {
        //暂时无动态参数，不开启
//        initFetchConfigTask();
        this.agentHome = agentHome;
        this.configFilePath = agentHome + File.separator + CONFIG_PATH_NAME;
        this.providerFilePath = agentHome + File.separator + PROVIDER_PATH_NAME;
        this.simulatorHome = agentHome + File.separator + AGENT_PATH_NAME;
        this.simulatorJarPath = this.simulatorHome + File.separator + "simulator" + File.separator + "instrument-simulator-agent.jar";
        this.logConfigFilePath = this.configFilePath + File.separator + "simulator-agent-logback.xml";
        File configFile = new File(configFilePath, "agent.properties");
        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        InputStream configIn = null;
        try {
            if (!configFile.exists() || !configFile.canRead()) {
                configIn = CoreConfig.class.getClassLoader().getResourceAsStream("agent.properties");
            } else {
                configIn = new FileInputStream(configFile);
            }

            Enumeration enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String)enumeration.nextElement();
                configs.put(name, properties.getProperty(name));
            }
            properties.clear();
            properties.load(configIn);
            enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String)enumeration.nextElement();
                agentFileConfigs.put(name, properties.getProperty(name));
            }
            configs.putAll(agentFileConfigs);
        } catch (Throwable e) {
            throw new RuntimeException("Agent: read agent.properties file err:" + configFile.getAbsolutePath(), e);
        } finally {
            if (configIn != null) {
                try {
                    configIn.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void initFetchConfigTask() {
        this.service = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Pradar-agent-Fetch-Config-Service");
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        logger.error("Thread {} caught a Unknown exception with UncaughtExceptionHandler", t.getName(),
                            e);
                    }
                });
                return t;
            }
        });

        service.scheduleAtFixedRate(getRunnableTask(), 60 * 3, 60 * 3, TimeUnit.SECONDS);
    }

    private Runnable getRunnableTask() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> dynamicConfigs = ConfigUtils.getDynamicAgentConfigFromUrl(getTroWebUrl(),
                        getAppName(), "", getUserAppKey());
                    if (dynamicConfigs == null || dynamicConfigs.get("success") == null || !Boolean.valueOf(
                        dynamicConfigs.get("success").toString())) {
                        logger.error("getDynamicAgentConfigFromUrl failed");
                        return;
                    }
                    JSONObject jsonObject = (JSONObject)dynamicConfigs.get("data");
                    //                    for (Map.Entry<String, String> entry : jsonObject){
                    //                        if (!agentFileConfigs.containsKey(entry.getKey())){
                    //                            configs.put(entry.getKey(), entry.getValue());
                    //                        }
                    //                    }
                } catch (Throwable e) {
                    logger.error("CoreConfig getRunnableTask error {}", e);
                }
            }
        };
    }

    public boolean isMultiAppSwitch() {
        String value = configs.get(MULTI_APP_SWITCH);
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取日志级别
     *
     * @return 日志级别
     */
    public String getLogLevel() {
        String level = configs.get(LOG_LEVEL_NAME);
        if (StringUtils.isBlank(level)) {
            return DEFAULT_LOG_LEVEL;
        }
        return StringUtils.trim(level);
    }

    /**
     * 获取日志路径,日志路径如果不包含应用名称，则自动加上应用名称
     *
     * @return 日志路径
     */
    public String getLogPath() {
        String path = configs.get(LOG_PATH_NAME);
        if (StringUtils.isNotBlank(path)) {
            String cpath = path;
            if (!StringUtils.endsWith(cpath, "/")) {
                cpath += "/";
            }
            String appName = getAppName();
            /**
             * 这样判断是防止有路径包含了应用名称的字母但是不是应用名为目录
             */
            if (StringUtils.isNotBlank(appName) && StringUtils.indexOf(cpath, "/" + appName + "/") == -1) {
                cpath += appName;
                return isMultiAppSwitch() ? cpath + '/' + AddressUtils.getLocalAddress() + '/' + PidUtils.getPid()
                    : cpath;
            }
            return isMultiAppSwitch() ? path + '/' + AddressUtils.getLocalAddress() + '/' + PidUtils.getPid() : path;
        }
        String value = System.getProperty("user.home") + File.separator + "pradarlogs" + File.separator + getAppName();
        if (isMultiAppSwitch()) {
            value += '/' + PidUtils.getPid();
        }
        return value;
    }

    /**
     * 获取 boolean类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(configs.get(propertyName));
    }

    /**
     * 获取 int 类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public int getIntProperty(String propertyName, int defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        String value = StringUtils.trim(configs.get(propertyName));
        if (NumberUtils.isDigits(value)) {
            return Integer.parseInt(value);
        }
        return defaultValue;
    }

    /**
     * 获取 long 类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public long getLongProperty(String propertyName, long defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        String value = StringUtils.trim(configs.get(propertyName));
        if (NumberUtils.isDigits(value)) {
            return Integer.parseInt(value);
        }
        return defaultValue;
    }

    /**
     * 获取属性配置值
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public String getProperty(String propertyName, String defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        return StringUtils.trim(configs.get(propertyName));
    }

    /**
     * 获取 agent home 目录地址
     *
     * @return agent home
     */
    public String getAgentHome() {
        return agentHome;
    }

    /**
     * 获取配置文件路径
     *
     * @return config file path
     */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * 获取 spi 目录路径
     *
     * @return spi file path
     */
    public String getProviderFilePath() {
        return providerFilePath;
    }

    /**
     * 获取 agent 目录路径
     *
     * @return agent file path
     */
    public String getSimulatorHome() {
        return simulatorHome;
    }

    /**
     * 获取 agent jar 路径
     *
     * @return agent jar path
     */
    public String getSimulatorJarPath() {
        return simulatorJarPath;
    }

    /**
     * 获取 zk 地址
     *
     * @return
     */
    public String getZkServers() {
        return getProperty("simulator.zk.servers", "localhost:2181");
    }

    /**
     * 获取zk 注册路径
     *
     * @return
     */
    public String getRegisterPath() {
        return getProperty("simulator.client.zk.path", "/config/log/pradar/client");
    }

    /**
     * 获取 zk 连接超时时间
     *
     * @return
     */
    public int getZkConnectionTimeout() {
        String connectionTimeout = getProperty("simulator.zk.connection.timeout.ms", "30000");
        if (NumberUtils.isDigits(connectionTimeout)) {
            return Integer.parseInt(connectionTimeout);
        }
        return 60000;
    }

    /**
     * 获取 zk session 超时时间
     *
     * @return
     */
    public int getZkSessionTimeout() {
        String sessionTimeout = getProperty("simulator.zk.session.timeout.ms", "60000");
        if (NumberUtils.isDigits(sessionTimeout)) {
            return Integer.parseInt(sessionTimeout);
        }
        return 60000;
    }

    /**
     * 获取应用名称
     *
     * @return 应用名称
     */
    public String getAppName() {
        String value = getPropertyInAll("simulator.app.name");
        if (StringUtils.isBlank(value)) {
            value = getPropertyInAll("pradar.project.name");
        }
        if (StringUtils.isBlank(value)) {
            value = getPropertyInAll("app.name");
        }
        return value != null ? value : "default";
    }

    private String getPropertyInAll(String key) {
        String value = System.getProperty(key);
        if (StringUtils.isBlank(value)) {
            value = getProperty(key, null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv(key);
        }
        return value;
    }

    /**
     * 获取 agentId
     *
     * @return 获取 agentId
     */
    public String getAgentId() {
        String agentId = internalGetAgentId();
        if (StringUtils.isBlank(agentId)) {
            return new StringBuilder(AddressUtils.getLocalAddress()).append("-").append(PidUtils.getPid()).toString();
        } else {
            Properties properties = new Properties();
            properties.setProperty("pid", String.valueOf(PidUtils.getPid()));
            properties.setProperty("hostname", AddressUtils.getHostName());
            properties.setProperty("ip", AddressUtils.getLocalAddress());
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            return propertyPlaceholderHelper.replacePlaceholders(agentId, properties);
        }
    }

    private String internalGetAgentId() {
        String value = System.getProperty("simulator.agentId");
        if (StringUtils.isBlank(value)) {
            value = System.getProperty("pradar.agentId");
        }
        if (StringUtils.isBlank(value)) {
            value = getProperty("simulator.agentId", null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv("simulator.agentId");
        }
        return value;
    }

    public String getUserAppKey() {
        String value = System.getProperty("user.app.key");
        if (StringUtils.isBlank(value)) {
            value = getProperty("user.app.key", null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv("user.app.key");
        }
        return value;
    }

    public String getTroWebUrl() {
        String value = System.getProperty("tro.web.url");
        if (StringUtils.isBlank(value)) {
            value = getProperty("tro.web.url", null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv("tro.web.url");
        }
        return value;
    }

    public String getUserId() {
        String value = System.getProperty("pradar.user.id");
        if (StringUtils.isBlank(value)) {
            value = getProperty("pradar.user.id", null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv("pradar.user.id");
        }
        return value;
    }

    /**
     * 获取 agent结果文件路径
     *
     * @return
     */
    public String getAgentResultFilePath() {
        return String.format(RESULT_FILE_PATH, getAppName());
    }

    /**
     * 获取 log 配置文件路径
     *
     * @return
     */
    public String getLogConfigFilePath() {
        return logConfigFilePath;
    }

    public InputStream getLogConfigFile() {
        File file = new File(getLogConfigFilePath());
        if (!file.exists()) {
            return CoreConfig.class.getClassLoader().getResourceAsStream("simulator-agent-logback.xml");
        } else {
            try {
                return new FileInputStream(getLogConfigFilePath());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setAttachId(long attachId) {
        this.attachId = attachId;
    }

    public void setAttachName(String attachName) {
        this.attachName = attachName;
    }

    public long getAttachId() {
        return this.attachId;
    }

    public String getAttachName() {
        return this.attachName;
    }

    public Map<String, String> getAgentFileConfigs() {
        return agentFileConfigs;
    }
}
