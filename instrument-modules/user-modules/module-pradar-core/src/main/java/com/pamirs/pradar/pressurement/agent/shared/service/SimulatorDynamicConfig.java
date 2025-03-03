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
package com.pamirs.pradar.pressurement.agent.shared.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author angju
 * @date 2021/8/17 17:35
 */
public class SimulatorDynamicConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorDynamicConfig.class.getName());

    /**
     * 默认业务流量采样率,默认为1/9999
     */
    public static final int DEFAULT_TRACE_SAMPLING_INTERVAL = 9999;
    /**
     * 默认压测流量采样率，默认为1/1
     */
    public static final int DEFAULT_CLUSTER_TEST_TRACE_SAMPLING_INTERVAL = 1;

    private static final String PRADAR_TRACE_LOG_VERSION_KEY = "pradar.trace.log.version";
    private static final String PRADAR_MONITOR_LOG_VERSION_KEY = "pradar.monitor.log.version";
    private static final String PRADAR_ERROR_LOG_VERSION_KEY = "pradar.error.log.version";
    private static final String IS_KAFKA_MESSAGE_HEADERS_KEY = "is.kafka.message.headers";
    private static final String TRACE_SAMPLING_INTERVAL_KEY = "trace.samplingInterval";
    private static final String CLUSTER_TEST_TRACE_SAMPLING_INTERVAL_KEY = "trace.ct.samplingInterval";
    private static final String SWITCH_SAVE_BUSINESS_TRACE_KEY = "pradar.switch.save.business.trace";
    private static final String BUS_REQUEST_RESPONSE_DATA_ALLOW_TRACE = "pradar.bus.request.response.data.allow.trace";
    private static final String SHADOW_REQUEST_RESPONSE_DATA_ALLOW_TRACE = "pradar.shadow.request.response.data.allow.trace";

    /**
     * trace 业务流量采样率
     */
    private final int traceSamplingInterval;
    /**
     * trace 压测流量采样率
     */
    private final int clusterTestTraceSamplingInterval;
    /**
     * trace 日志 的版本号
     */
    private final Integer pradarTraceLogVersion;
    /**
     * monitor 日志的版本号
     */
    private final Integer pradarMonitorLogVersion;
    /**
     * error 日志的版本号
     */
    private final Integer pradarErrorLogVersion;
    /**
     * 是否使用 kafka 消息头传递 trace 上下文
     */
    private final Boolean isKafkaMessageHeader;
    /**
     * 是否关闭所有的业务流量执行
     */
    private final boolean isSwitchSaveBusinessTrace;

    /**
     * 业务请求响应是否采集 默认不采集
     */
    private final boolean busRequestResponseDataAllowTrace;

    /**
     * 压测请求响应是否采集 默认不采集
     */
    private final boolean shadowRequestResponseDataAllowTrace;

    private Map<String, String> config = null;

    public SimulatorDynamicConfig(Map<String, String> config) {
        this.config = config;
        for (Map.Entry<String, String> entry : config.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
        this.traceSamplingInterval = getTraceSamplingInterval(config);
        this.clusterTestTraceSamplingInterval = getClusterTestTraceSamplingInterval(config);
        this.pradarTraceLogVersion = getPradarTraceLogVersion(config);
        this.pradarMonitorLogVersion = getPradarMonitorLogVersion(config);
        this.pradarErrorLogVersion = getPradarErrorLogVersion(config);
        this.isKafkaMessageHeader = getIsKafkaMessageHeaders(config);
        this.isSwitchSaveBusinessTrace = getSwitchSaveBusinessTrace(config);
        this.busRequestResponseDataAllowTrace = getBusRequestResponseDataAllowTrace(config);
        this.shadowRequestResponseDataAllowTrace = getShadowRequestResponseDataAllowTrace(config);
    }


    public boolean isShadowRequestResponseDataAllowTrace() {
        return shadowRequestResponseDataAllowTrace;
    }

    public boolean isBusRequestResponseDataAllowTrace() {
        return busRequestResponseDataAllowTrace;
    }

    /**
     * trace日志推送版本
     *
     * @return
     */
    public Integer getPradarTraceLogVersion(Map<String, String> config) {
        try {
            if (config == null) {
                return null;
            }
            String data = getConfig(config, PRADAR_TRACE_LOG_VERSION_KEY);
            if (data == null) {
                return null;
            }
            return Integer.valueOf(data.replace(".", ""));
        } catch (Exception e) {
            LOGGER.error("getPradarTraceLogVersion error {}", e);
            return null;
        }
    }

    /**
     * trace日志推送版本
     *
     * @param defaultVersion
     * @return
     */
    public int getPradarTraceLogVersion(int defaultVersion) {
        return pradarTraceLogVersion == null ? defaultVersion : pradarTraceLogVersion;
    }

    /**
     * monitor日志推送版本
     *
     * @return
     */
    public Integer getPradarMonitorLogVersion(Map<String, String> config) {
        try {
            if (config == null) {
                return null;
            }
            String data = getConfig(config, PRADAR_MONITOR_LOG_VERSION_KEY);
            if (data == null) {
                return null;
            }
            return Integer.valueOf(data.replace(".", ""));
        } catch (Exception e) {
            LOGGER.error("getPradarMonitorLogVersion error {}", e);
            return null;
        }
    }

    /**
     * monitor日志推送版本
     *
     * @param defaultVersion
     * @return
     */
    public int getPradarMonitorLogVersion(int defaultVersion) {
        return pradarMonitorLogVersion == null ? defaultVersion : pradarMonitorLogVersion;
    }


    public Integer getPradarErrorLogVersion(Map<String, String> config) {
        try {
            if (config == null) {
                return null;
            }
            String data = getConfig(config, PRADAR_ERROR_LOG_VERSION_KEY);
            if (data == null) {
                return null;
            }
            return Integer.valueOf(data);
        } catch (Exception e) {
            LOGGER.error("getPradarErrorLogVersion error {}", e);
            return null;
        }
    }

    public int getPradarErrorLogVersion(int defaultVersion) {
        return pradarErrorLogVersion == null ? defaultVersion : pradarErrorLogVersion;
    }

    /**
     * kafka是否可以用header
     *
     * @return
     */
    public Boolean getIsKafkaMessageHeaders(Map<String, String> config) {
        try {
            if (config == null) {
                return null;
            }
            String data = getConfig(config, IS_KAFKA_MESSAGE_HEADERS_KEY);
            if (data == null) {
                return null;
            }
            return Boolean.valueOf(data);
        } catch (Exception e) {
            LOGGER.error("getIsKafkaMessageHeaders error {}", e);
            return null;
        }
    }

    /**
     * kafka是否可以用header
     *
     * @param defaultValue
     * @return
     */
    public boolean getIsKafkaMessageHeaders(boolean defaultValue) {
        return isKafkaMessageHeader == null ? defaultValue : isKafkaMessageHeader;
    }

    /**
     * 是否为了性能不执行业务流量的trace和隔离逻辑
     *
     * @return
     */
    public boolean getSwitchSaveBusinessTrace(Map<String, String> config) {
        try {
            if (config == null) {
                return false;
            }
            String data = getConfig(config, SWITCH_SAVE_BUSINESS_TRACE_KEY);
            if (data == null) {
                return false;
            }
            return Boolean.parseBoolean(data);
        } catch (Exception e) {
            LOGGER.error("getIsKafkaMessageHeaders error, use default false.", e);
            return false;
        }
    }

    /**
     * 是否为了性能不执行业务流量的trace和隔离逻辑
     *
     * @return
     */
    public boolean isSwitchSaveBusinessTrace() {
        return isSwitchSaveBusinessTrace;
    }

    private int getTraceSamplingInterval(Map<String, String> config) {
        try {
            if (config == null) {
                return DEFAULT_TRACE_SAMPLING_INTERVAL;
            }
            final String value = getConfig(config, TRACE_SAMPLING_INTERVAL_KEY);
            if (value == null) {
                return DEFAULT_TRACE_SAMPLING_INTERVAL;
            }
            int traceSamplingInterval = Integer.valueOf(value);
            return traceSamplingInterval;
        } catch (Throwable e) {
            LOGGER.error("getTraceSamplingInterval error {}.", e);
            return DEFAULT_TRACE_SAMPLING_INTERVAL;
        }
    }

    /**
     * trace日志采样率
     *
     * @return
     */
    public int getClusterTestTraceSamplingInterval(Map<String, String> config) {
        try {
            if (config == null) {
                return DEFAULT_CLUSTER_TEST_TRACE_SAMPLING_INTERVAL;
            }
            final String value = getConfig(config, CLUSTER_TEST_TRACE_SAMPLING_INTERVAL_KEY);
            if (value == null) {
                return DEFAULT_CLUSTER_TEST_TRACE_SAMPLING_INTERVAL;
            }
            int traceSamplingInterval = Integer.valueOf(value);
            return traceSamplingInterval;
        } catch (Exception e) {
            LOGGER.error("getClusterTestTraceSamplingInterval error {}.", e);
            return DEFAULT_CLUSTER_TEST_TRACE_SAMPLING_INTERVAL;
        }
    }

    /**
     * trace日志采样率
     *
     * @return
     */
    public int getTraceSamplingInterval() {
        return traceSamplingInterval;
    }

    /**
     * trace日志采样率
     *
     * @return
     */
    public int getClusterTestTraceSamplingInterval() {
        return clusterTestTraceSamplingInterval;
    }

    private String getConfig(Map<String, String> config, String key) {
        String s = config.get(key);
        return s == null ? System.getProperty(key) : s;
    }


    /**
     * 是否允许采集业务的请求体、响应体
     *
     * @return
     */
    private boolean getBusRequestResponseDataAllowTrace(Map<String, String> config) {
        try {
            if (config == null) {
                return false;
            }
            String data = getConfig(config, BUS_REQUEST_RESPONSE_DATA_ALLOW_TRACE);
            if (data == null) {
                return false;
            }
            return Boolean.valueOf(data);
        } catch (Exception e) {
            LOGGER.error("getBusRequestResponseDataAllowTrace error {}, use default false.", e);
            return false;
        }
    }


    /**
     * 是否允许采集压测的请求体、响应体
     *
     * @return
     */
    private boolean getShadowRequestResponseDataAllowTrace(Map<String, String> config) {
        try {
            if (config == null) {
                return false;
            }
            String data = getConfig(config, SHADOW_REQUEST_RESPONSE_DATA_ALLOW_TRACE);
            if (data == null) {
                return false;
            }
            return Boolean.valueOf(data);
        } catch (Exception e) {
            LOGGER.error("getShadowRequestResponseDataAllowTrace error {}, use default false.", e);
            return false;
        }
    }
}
