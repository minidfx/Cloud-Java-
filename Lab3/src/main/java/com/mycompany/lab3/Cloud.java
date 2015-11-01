package com.mycompany.lab3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;

/**
 * Represents the service responsible for creating an MongoDB instance.
 *
 * @author Burgy Benjamin
 */
public interface Cloud
{
    /**
     * Creates a MongoDB instance.
     *
     * @throws java.lang.Exception
     */
    void create() throws Exception;

    /**
     * Destroy the MongoDB instance.
     */
    void destroy();

    /**
     * Releases connections to the cloud.
     *
     * @throws java.lang.Exception
     */
    void release() throws Exception;

    /**
     * Waits for the instance specified as argument is running.
     *
     * @param instanceName
     * @param client
     *
     * @throws InterruptedException
     */
    default void WaitForInstance(String instanceName, ComputeService client) throws InterruptedException
    {
        List<ComputeMetadata> nodes = new ArrayList(client.listNodes());

        Optional<ComputeMetadata> mongoDbNode = nodes.stream()
                .filter(x -> x.getName().equals(instanceName) && client.getNodeMetadata(x.getId()).getStatus() == NodeMetadata.Status.RUNNING)
                .findFirst();

        while (!mongoDbNode.isPresent())
        {
            mongoDbNode = nodes.stream()
                    .filter(x -> x.getName().equals(instanceName) && client.getNodeMetadata(x.getId()).getStatus() == NodeMetadata.Status.RUNNING)
                    .findFirst();

            Thread.sleep(1000);
        }
    }
}
