package com.peregrine.replication.impl;

/*-
 * #%L
 * admin base - Core
 * %%
 * Copyright (C) 2017 headwire inc.
 * %%
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */

import com.peregrine.replication.ReplicationServiceBase;
import com.peregrine.commons.util.PerConstants;
import com.peregrine.commons.util.PerUtil;
import com.peregrine.reference.ReferenceLister;
import com.peregrine.replication.Replication;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.peregrine.replication.ReplicationUtil.updateReplicationProperties;
import static org.apache.sling.distribution.DistributionRequestState.ACCEPTED;
import static org.apache.sling.distribution.DistributionRequestState.DISTRIBUTED;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

/**
 * This service is replicating resource to a remote sling instance
 * using the Sling Distribution's Forward Agent on the local Sling instance
 * and an Importer Service on the remove Sling instance.
 */
@Component(
    configurationPolicy = REQUIRE,
    service = Replication.class,
    immediate = true
)
@Designate(ocd = DistributionReplicationService.Configuration.class, factory = true)
public class DistributionReplicationService
    extends ReplicationServiceBase
{

    public static final String DISTRIBUTION_PENDING = "distribution pending";
    public static final String NO_DISTRIBUTOR_AVAILABLE = "No Distributor available -> configure Sling Distribution first";
    public static final String DISTRIBUTION_FAILED = "Distribution failed due to: '%s'";

    @ObjectClassDefinition(
        name = "Peregrine: Remote Replication Service",
        description = "Each instance provides the configuration for Remote Replication through Sling Distribution"
    )
    @interface Configuration {
        @AttributeDefinition(
            name = "Name",
            description = "Name of the Replication Service"
        )
        String name();
        @AttributeDefinition(
            name = "Description",
            description = "Description of this Replication Service"
        )
        String description();
        @AttributeDefinition(
            name = "Forward Agent",
            description = "Name of the Forward Agent to use for the Replication."
        )
        String agentName();
    }
    @Activate
    @SuppressWarnings("unused")
    void activate(Configuration configuration) { setup(configuration); }
    @Modified
    @SuppressWarnings("unused")
    void modified(Configuration configuration) { setup(configuration); }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    Distributor distributor;

    private String agentName;

    private void setup(Configuration configuration) {
        init(configuration.name(), configuration.description());
        log.trace("Distributor: '{}'", distributor);
        agentName = configuration.agentName();
        if(StringUtils.isEmpty(agentName)) {
            throw new IllegalArgumentException("Agent Name must be provided");
        }
    }

    @Reference
    @SuppressWarnings("unused")
    private ReferenceLister referenceLister;

    @Override
    public List<Resource> findReferences(Resource startingResource, boolean deep, PerUtil.ResourceChecker checker) {
        log.trace("Starting Resource: '{}'", startingResource.getPath());
        List<Resource> referenceList = referenceLister.getReferenceList(true, startingResource, true, checker);
        log.trace("Reference List: '{}'", referenceList);
        List<Resource> replicationList = new ArrayList<>();
        // Need to check this list of they need to be replicated first
        for(Resource resource: referenceList) {
            replicationList.add(resource);
        }
        // This only returns the referenced resources. Now we need to check if there are any JCR Content nodes to be added as well
        for(Resource reference: new ArrayList<>(replicationList)) {
            PerUtil.listMissingResources(reference, checker, false, replicationList);
        }
        PerUtil.listMissingResources(startingResource, checker, deep, replicationList);
        log.trace("List for Replication: '{}'", replicationList);
        return replicationList;
    }

    @Override
    public List<Resource> deactivate(Resource startingResource)
        throws ReplicationException
    {
        log.trace("Starting Resource: '{}'", startingResource.getPath());
        List<Resource> replicationList = new ArrayList<>();
        PerUtil.listMissingResources(startingResource, true, replicationList);
        log.trace("List for Replication: '{}'", replicationList);
        return deactivate(replicationList);
    }

    @Override
    public List<Resource> replicate(Collection<Resource> resourceList) throws ReplicationException {
        return replicate(resourceList, true);
    }

    public List<Resource> deactivate(Collection<Resource> resourceList) throws ReplicationException {
        return replicate(resourceList, false);
    }

    public List<Resource> replicate(Collection<Resource> resourceList, boolean activate) throws ReplicationException {
        List<Resource> answer = new ArrayList<>();
        if(distributor != null) {
            ResourceResolver resourceResolver = null;
            for(Resource item: resourceList) {
                if(item != null) {
                    resourceResolver = item.getResourceResolver();
                    break;
                }
            }
            if(resourceResolver != null) {
                String[] paths = new String[resourceList.size()];
                int i = 0;
                for(Resource resource : resourceList) {
                    paths[i++] = resource.getPath();
                    // In order to make it possible to have the correct user set and 'Replicated By' we need to set it here and now
                    updateReplicationProperties(resource, DISTRIBUTION_PENDING, null);
                }

                if(distributor != null) {
                    if (activate) {
                        // first deactivate page content so deleted or moved content is cleared
                        String[] jcrPaths = Arrays.stream(paths)
                                .filter(path -> path.endsWith(PerConstants.JCR_CONTENT))
                                .toArray(String[]::new);
                        SimpleDistributionRequest sdrDeactivate = new SimpleDistributionRequest(
                                DistributionRequestType.DELETE,
                                jcrPaths);
                        DistributionResponse deactivateResp = distributor.distribute(
                            agentName,
                            resourceResolver,
                            sdrDeactivate
                        );

                        log.trace("Distributor Response: '{}'", deactivateResp);
                        if(!deactivateResp.isSuccessful() || !(deactivateResp.getState() == ACCEPTED || deactivateResp.getState() != DISTRIBUTED)) {
                            throw new ReplicationException(String.format(DISTRIBUTION_FAILED, deactivateResp));
                        }
                    }

                    // then activate/deactivate the nodes
                    DistributionResponse response = distributor.distribute(
                            agentName,
                            resourceResolver,
                            new SimpleDistributionRequest(
                                    activate ? DistributionRequestType.ADD : DistributionRequestType.DELETE,
                                    paths));
                    log.trace("Distributor Response: '{}'", response);
                    if(!response.isSuccessful() || !(response.getState() == ACCEPTED || response.getState() != DISTRIBUTED)) {
                        throw new ReplicationException(String.format(DISTRIBUTION_FAILED, response));
                    }

                    answer.addAll(resourceList);
                } else {
                    throw new ReplicationException(NO_DISTRIBUTOR_AVAILABLE);
                }
            }
        }
        return answer;
    }

}
