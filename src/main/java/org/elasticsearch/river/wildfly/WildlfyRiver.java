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

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.UUID;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;
import org.elasticsearch.threadpool.ThreadPool;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 *
 */
public class WildlfyRiver extends AbstractRiverComponent implements River {

    private final ThreadPool threadPool;

    private final Client client;
    private final org.elasticsearch.common.joda.time.format.DateTimeFormatter timeFormatter;

    private String indexName;
    private String typeName;


    private volatile boolean closed = false;
    private String username;
    private int port;
    private String host;
    private String password;
    private ScheduledFuture<?> work;
    private int scheduleSeconds;
    private ModelControllerClient controllerClient;

    @SuppressWarnings({"unchecked"})
    @Inject
    public WildlfyRiver(RiverName riverName, RiverSettings settings, Client client, ThreadPool threadPool) {
        super(riverName, settings);
        this.client = client;
        this.threadPool = threadPool;

        logger.info("Creating wildfly metric stream");

        indexName = riverName.name();
        typeName = "metrics";

        //dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

        timeFormatter = ISODateTimeFormat.dateTimeNoMillis();

    }


    static ModelControllerClient createClient(
            final InetAddress host, final int port,
            final String username, final String password) {

        final CallbackHandler callbackHandler = new CallbackHandler() {

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback current : callbacks) {
                    if (current instanceof NameCallback) {
                        NameCallback ncb = (NameCallback) current;
                        ncb.setName(username);
                    } else if (current instanceof PasswordCallback) {
                        PasswordCallback pcb = (PasswordCallback) current;
                        pcb.setPassword(password.toCharArray());
                    } else if (current instanceof RealmCallback) {
                        RealmCallback rcb = (RealmCallback) current;
                        rcb.setText(rcb.getDefaultText());
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }
            }
        };

        return ModelControllerClient.Factory.create(host, port, callbackHandler);
    }

    @Override
    public void start() {

        parseConfig();

        try {
            ModelNode op = new ModelNode();
            op.get("operation").set("read-attribute");
            op.get("address").setEmptyList();
            op.get("name").set("release-version");

            this.controllerClient = createDefaultClient();

            ModelNode response = controllerClient.execute(op);

            String result = response.get("result").toString();

            logger.info("Connected to Wildfly "+ result);

            work = scheduleWork();

        } catch (IOException e) {
            logger.error("Startup failed ", e);
        }

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

    private ModelControllerClient createDefaultClient() {

        ModelControllerClient client = null;
        try {

            client = createClient(
                                InetAddress.getByName(host),
                                port,
                                username,
                                password
                        );


        } catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host", e);
        }

        return client;
    }

    private ScheduledFuture<?> scheduleWork() {
        ScheduledFuture<?> future = threadPool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {

                // re-create if necessary
                if(controllerClient == null)
                {
                    controllerClient = createDefaultClient();
                }

                try {

                    // > /core-service=platform-mbean/type=memory:read-resource(include-runtime=true)

                    ModelNode op = new ModelNode();
                    op.get("operation").set("read-resource");
                    op.get("address").add("core-service","platform-mbean");
                    op.get("address").add("type", "memory");
                    op.get("include-runtime").set("true");


                    ModelNode response = controllerClient.execute(op);
                    ModelNode result = response.get("result");
                    //System.out.println(result);

                    ModelNode heap = result.get("heap-memory-usage");

                    XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
                    builder.field("@source", "wildfly-import");
                    //builder.field("@tags", builder.startArray().endArray());
                    builder.field("@timestamp", timeFormatter.print(new DateTime()));
                    builder.field("@message", result.toString());
                    builder.field("@type", "vm");
                    builder.field("@source_path", op.get("address").toString());

                    builder.startObject("@fields");
                        builder.field("used", heap.get("used").asLong());
                        builder.field("max", heap.get("max").asLong());
                    builder.endObject();

                    builder.endObject();

                    IndexResponse idx = client.prepareIndex(indexName, typeName)
                            .setSource(builder)
                            .execute()
                            .actionGet();

                    //System.out.println(">>"+idx.getId());


                } catch (IOException e) {
                    logger.error("Failed to execute operation", e);
                }


            }
        }, TimeValue.timeValueSeconds(scheduleSeconds));

        return future;
    }

    private void parseConfig() {
        if (settings.settings().containsKey("wildfly")) {
            Map<String, Object> wildflySettings = (Map<String, Object>) settings.settings().get("wildfly");

            this.username = XContentMapValues.nodeStringValue(wildflySettings.get("user"), null);
            this.password = XContentMapValues.nodeStringValue(wildflySettings.get("user"), null);
            this.host = XContentMapValues.nodeStringValue(wildflySettings.get("host"), null);
            this.port = XContentMapValues.nodeIntegerValue(wildflySettings.get("port"), 9999);
            this.scheduleSeconds = XContentMapValues.nodeIntegerValue(wildflySettings.get("schedule"), 1);

        }
        else
        {
            logger.error("invalid wildfly plugin configuration");
        }

        if (settings.settings().containsKey("index")) {
            Map<String, Object> indexSettings = (Map<String, Object>) settings.settings().get("index");
            indexName = XContentMapValues.nodeStringValue(indexSettings.get("index"), riverName.name());
            typeName = XContentMapValues.nodeStringValue(indexSettings.get("type"), "status");
        }
        else
        {
            logger.error("invalid wildfly plugin configuration");
        }
    }

    @Override
    public void close() {
        this.closed = true;
        if(this.work !=null)
        {
            this.work.cancel(true);
            this.work = null;
        }


        logger.info("closing wildfly metric stream");
    }

}
