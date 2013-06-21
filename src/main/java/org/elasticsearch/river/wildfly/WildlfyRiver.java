/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.river.wildfly;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;
import org.elasticsearch.threadpool.ThreadPool;

/**
 *
 */
public class WildlfyRiver extends AbstractRiverComponent implements River {

    private final ThreadPool threadPool;

    private final Client client;


    private final String indexName;

    private final String typeName;

    private volatile BulkRequestBuilder currentRequest;

    private volatile boolean closed = false;

    @SuppressWarnings({"unchecked"})
    @Inject
    public WildlfyRiver(RiverName riverName, RiverSettings settings, Client client, ThreadPool threadPool) {
        super(riverName, settings);
        this.client = client;
        this.threadPool = threadPool;

        logger.info("Creating wildlfy stream river");

        indexName = riverName.name();
        typeName = "metrics";

    }


    @Override
    public void start() {

        /*try {

            String mapping = XContentFactory.jsonBuilder().startObject().startObject(typeName).startObject("properties")
                    .startObject("location").field("type", "geo_point").endObject()
                    .startObject("user").startObject("properties").startObject("screen_name").field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject()
                    .startObject("mention").startObject("properties").startObject("screen_name").field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject()
                    .startObject("in_reply").startObject("properties").startObject("user_screen_name").field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject()
                    .endObject().endObject().endObject().string();
            client.admin().indices().prepareCreate(indexName).addMapping(typeName, mapping).execute().actionGet();

        } catch (Exception e) {
            if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
                // that's fine
            } else if (ExceptionsHelper.unwrapCause(e) instanceof ClusterBlockException) {
                // ok, not recovered yet..., lets start indexing and hope we recover by the first bulk
                // TODO: a smarter logic can be to register for cluster event listener here, and only start sampling when the block is removed...
            } else {
                logger.warn("failed to create index [{}], disabling river...", e, indexName);
                return;
            }
        }
        currentRequest = client.prepareBulk();      */
    }

    @Override
    public void close() {
        this.closed = true;
        logger.info("closing wildfly stream river");
    }

}
