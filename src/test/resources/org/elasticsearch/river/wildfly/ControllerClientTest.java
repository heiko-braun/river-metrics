package org.elasticsearch.river.wildfly;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import java.net.InetAddress;

/**
 * @author Heiko Braun
 * @date 6/21/13
 */
public class ControllerClientTest {


    @Test
    public void testConnection() throws Exception {
        ModelControllerClient client = WildlfyRiver.createClient(
                InetAddress.getByName("localhost"),
                9990,
                "admin",
                "test123!"
        );

        ModelNode op = new ModelNode();
        op.get("operation").set("read-attribute");
        op.get("address").setEmptyList();
        op.get("name").set("release-version");

        ModelNode response = client.execute(op);
        String result = response.get("result").toString();
        System.out.println(response.toString());

    }
}
