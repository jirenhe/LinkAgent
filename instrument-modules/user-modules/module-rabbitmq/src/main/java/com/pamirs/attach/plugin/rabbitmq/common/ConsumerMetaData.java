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
package com.pamirs.attach.plugin.rabbitmq.common;

import com.pamirs.pradar.Pradar;
import com.rabbitmq.client.Consumer;

import java.util.Collections;
import java.util.Map;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/19 5:54 下午
 */
public class ConsumerMetaData {

    private String queue;
    private String ptQueue;
    private final String consumerTag;
    private final String ptConsumerTag;
    private final Consumer consumer;
    private final boolean exclusive;
    private final boolean autoAck;
    private final int prefetchCount;
    private final boolean noLocal;
    private final Map<String, Object> arguments;
    private boolean routingKeyExchangeModel = false;

    public boolean isRoutingKeyExchangeModel() {
        return routingKeyExchangeModel;
    }

    public void setRoutingKeyExchangeModel(boolean routingKeyExchangeModel) {
        this.routingKeyExchangeModel = routingKeyExchangeModel;
    }

    public ConsumerMetaData(String queue, String consumerTag, Consumer consumer, boolean exclusive, boolean autoAck,
                            int prefetchCount, boolean noLocal) {
        this.queue = queue;
        this.consumerTag = consumerTag;
        this.consumer = consumer;
        this.exclusive = exclusive;
        this.autoAck = autoAck;
        this.prefetchCount = prefetchCount;
        this.noLocal = noLocal;
        this.arguments = Collections.emptyMap();
        if (queue != null) {
            this.ptQueue = Pradar.addClusterTestPrefix(queue);
        }
        this.ptConsumerTag = Pradar.addClusterTestPrefix(consumerTag);
    }

    public String getQueue() {
        return queue;
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean isAutoAck() {
        return autoAck;
    }

    public void setQueue(String queue) {
        this.ptQueue  = Pradar.addClusterTestPrefix(queue);
        this.queue = queue;
    }

    public int getPrefetchCount() {
        return prefetchCount;
    }

    public boolean isNoLocal() {
        return noLocal;
    }

    public String getPtQueue() {
        return ptQueue;
    }

    public String getPtConsumerTag() {
        return ptConsumerTag;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }
}
