package com.mycompany.lab3;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.Location;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;

/**
 *
 * @author Burgy Benjamin
 * @see
 * https://jclouds.apache.org/reference/javadoc/1.9.x/org/jclouds/openstack/nova/v2_0/compute/options/NovaTemplateOptions.html
 * @see https://jclouds.apache.org/guides/openstack/
 */
public class OpenStackImpl implements Cloud
{
    private final ComputeServiceContext computeApi;

    public OpenStackImpl()
    {
        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());

        Properties overrides = new Properties();
        overrides.setProperty(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
        overrides.setProperty(Constants.PROPERTY_API_VERSION, "2");

        this.computeApi = ContextBuilder.newBuilder("openstack-nova")
                .endpoint(CloudSettings.OpenStack_auth_url)
                .credentials(String.format("%s:%s", CloudSettings.OpenStack_tenant_name, CloudSettings.OpenStack_user), CloudSettings.OpenStack_password)
                .modules(modules)
                .overrides(overrides)
                .buildApi(ComputeServiceContext.class);
    }

    @Override
    public void create() throws Exception
    {
        ComputeService client = this.computeApi.getComputeService();

        List<ComputeMetadata> nodes = new ArrayList(client.listNodes());
        List<Hardware> hardwareProfiles = new ArrayList(client.listHardwareProfiles());
        List<Image> images = new ArrayList(client.listImages());
        List<Location> locations = new ArrayList(client.listAssignableLocations());

        Optional<Image> image = images.stream()
                .filter(x -> x.getName().equals("Ubuntu Trusty 14.04 (SWITCHengines)") && x.getLocation().getId().equals("ZH"))
                .findFirst();

        Optional<Hardware> hardwareProfile = hardwareProfiles.stream()
                .filter(x -> x.getName().equals("c1.micro") && x.getLocation().getId().equals("ZH"))
                .findFirst();

        if (image.isPresent() && hardwareProfile.isPresent())
        {
            Template ubuntuTemplate = client.templateBuilder()
                    .fromImage(image.get())
                    .hardwareId(hardwareProfile.get().getId())
                    .build();

            List<String> nodeNames = new ArrayList();
            nodeNames.add("MongoDB");

            NovaTemplateOptions templateOptions = ubuntuTemplate.getOptions().as(NovaTemplateOptions.class);
            templateOptions.securityGroups("anywhere");
            templateOptions.networks("685a78ae-602e-4b80-9b54-97bdfcef5c2f");
            templateOptions.keyPairName("switch-engine");
            templateOptions.floatingIpPoolNames("public");
            templateOptions.autoAssignFloatingIp(true);
            templateOptions.nodeNames(nodeNames);

            System.out.println("Creating a MongoDB server using Ubuntu Server Trusty ...");

            Set<? extends NodeMetadata> node = client.createNodesInGroup("lab3", 1, ubuntuTemplate);
            this.WaitForInstance("MongoDB", client);

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
        System.out.println("Destroying the MongoDB instance ...");
        this.computeApi.getComputeService().destroyNodesMatching(x -> x.getName().equals("MongoDB"));
        System.out.println("Done.");
    }

    @Override
    public void release() throws IOException
    {
        this.computeApi.close();
    }
}
