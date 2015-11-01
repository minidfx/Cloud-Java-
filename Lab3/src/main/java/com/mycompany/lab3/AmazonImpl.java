package com.mycompany.lab3;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.AWSEC2Api;
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.Location;
import org.jclouds.ec2.features.ElasticIPAddressApi;
import org.jclouds.sshj.config.SshjSshClientModule;

/**
 *
 * @author Burgy Benjamin
 * @see https://jclouds.apache.org/guides/aws-ec2/
 */
public class AmazonImpl implements Cloud
{
    private final AWSEC2Api awsec2Api;
    private final ComputeServiceContext computeApi;
    private ElasticIPAddressApi elasticAPI;
    private String elasticIP;

    public AmazonImpl() throws Exception
    {
        System.out.println("Creating Amazon context ...");

        Iterable<Module> modules = ImmutableSet.<Module>of(new SshjSshClientModule());

        Properties overrides = new Properties();
        overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_OWNERS, "363875371444");

        this.computeApi = ContextBuilder.newBuilder("aws-ec2")
                .credentials(CloudSettings.Amazon_accessKey, CloudSettings.Amazon_secretKey)
                .modules(modules)
                .buildApi(ComputeServiceContext.class);

        this.awsec2Api = this.computeApi.unwrapApi(AWSEC2Api.class);

        com.google.common.base.Optional<? extends ElasticIPAddressApi> elasticApi = this.awsec2Api.getElasticIPAddressApi();
        if (!elasticApi.isPresent())
        {
            throw new Exception("Cannot attach an elastic IP address!");
        }

        this.elasticAPI = elasticApi.get();

        System.out.println("Done.");
    }

    @Override
    public void create() throws Exception
    {
        System.out.println("Creating compute service ...");

        ComputeService client = this.computeApi.getComputeService();

        System.out.println("Done.");
        System.out.println("Reading cloud infrastructure ...");

        List<ComputeMetadata> nodes = new ArrayList(client.listNodes());
        List<Hardware> hardwareProfiles = new ArrayList(client.listHardwareProfiles());
        List<Image> images = new ArrayList(client.listImages());
        List<Location> locations = new ArrayList(client.listAssignableLocations());

        System.out.println("Done.");
//        System.out.println("Hardware Profiles");

//        for (Hardware hardwareProfile : hardwareProfiles)
//        {
//            System.out.println(hardwareProfile.getId());
//            System.out.println(hardwareProfile.getName());
//        }
//        System.out.println("Images");
//        for (Image image : images)
//        {
//            System.out.println(image.getId());
//        }
//        
        Optional<Image> image = images.stream()
                .filter(x -> x.getId().equals("eu-central-1/ami-accff2b1"))
                .findFirst();

        Optional<Hardware> hardwareProfile = hardwareProfiles.stream()
                .filter(x -> x.getId().equals("t2.micro"))
                .findFirst();

        if (image.isPresent() && hardwareProfile.isPresent())
        {
            Template ubuntuTemplate = client.templateBuilder()
                    .fromImage(image.get())
                    .hardwareId(hardwareProfile.get().getId())
                    .build();

            List<String> nodeNames = new ArrayList();
            nodeNames.add("MongoDB");

            AWSEC2TemplateOptions templateOptions = ubuntuTemplate.getOptions().as(AWSEC2TemplateOptions.class);
            templateOptions.securityGroups("sg-d35431ba");
            templateOptions.keyPair("EU-Project1");
            templateOptions.nodeNames(nodeNames);

            System.out.println("Creating a MongoDB server using Ubuntu Server Trusty ...");

            Set<? extends NodeMetadata> node = client.createNodesInGroup("lab3", 1, ubuntuTemplate);
            ComputeMetadata instance = this.WaitForInstance("MongoDB", client);

            System.out.println("Done.");
            System.out.println("Attaching an Elastic IP ...");

            com.google.common.base.Optional<? extends ElasticIPAddressApi> elasticApi = this.awsec2Api.getElasticIPAddressApi();

            this.elasticIP = elasticApi.get().allocateAddressInRegion("eu-central-1");
            elasticApi.get().associateAddressInRegion("eu-central-1", this.elasticIP, instance.getId());

            System.out.println("Done.");
        }
        else
        {
            throw new Exception("Configuration not found to start the Node.");
        }
    }

    @Override
    public void destroy()
    {
        System.out.println("Releasing Elastic IP adress ...");

        this.elasticAPI.disassociateAddressInRegion("eu-central-1", this.elasticIP);

        System.out.println("Done.");
        System.out.println("Destroying the MongoDB instance ...");

        this.computeApi.getComputeService().destroyNodesMatching(x -> x.getName().equals("MongoDB"));

        System.out.println("Done.");
    }

    @Override
    public void release() throws Exception
    {
        this.computeApi.close();
    }
}
