package com.peregrine.admin.servlets;

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

import com.peregrine.admin.resource.AdminResourceHandler;
import com.peregrine.admin.resource.AdminResourceHandler.ManagementException;
import com.peregrine.commons.servlets.AbstractBaseServlet;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;

import static com.peregrine.admin.servlets.AdminPathConstants.RESOURCE_TYPE_CREATION_OBJECT;
import static com.peregrine.commons.util.PerConstants.CREATED;
import static com.peregrine.commons.util.PerConstants.NAME;
import static com.peregrine.commons.util.PerConstants.PATH;
import static com.peregrine.commons.util.PerConstants.STATUS;
import static com.peregrine.commons.util.PerConstants.TEMPLATE_PATH;
import static com.peregrine.commons.util.PerConstants.TYPE;
import static com.peregrine.commons.util.PerUtil.EQUALS;
import static com.peregrine.commons.util.PerUtil.PER_PREFIX;
import static com.peregrine.commons.util.PerUtil.PER_VENDOR;
import static com.peregrine.commons.util.PerUtil.POST;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

@SuppressWarnings("serial")
public abstract class AbstractAdminServlet extends AbstractBaseServlet {

    public static final String OBJECT = "object";
    public static final String FAILED_TO_CREATE_OBJECT = "Failed to create object";

    transient ModelFactory modelFactory;

    transient AdminResourceHandler resourceManagement;

    public void setModelFactory(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    public void setResourceManagement(AdminResourceHandler resourceManagement) {
        this.resourceManagement = resourceManagement;
    }

    protected abstract String getType();
    protected abstract String getStatus();
    protected abstract String getFailureMessage();

    protected void enhanceResponse(JsonResponse response, Request request) throws IOException {};
}

