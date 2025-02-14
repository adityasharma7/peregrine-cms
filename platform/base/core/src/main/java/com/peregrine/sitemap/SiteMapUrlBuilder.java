package com.peregrine.sitemap;

/*-
 * #%L
 * platform base - Core
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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import static com.peregrine.commons.Strings.DOT;
import static com.peregrine.sitemap.SiteMapConstants.SITE_MAP;
import static com.peregrine.sitemap.SiteMapConstants.XML;

public interface SiteMapUrlBuilder {

    default String getFileName(int index) {
        if (index > 0) {
            return SITE_MAP + DOT + index + DOT + XML;
        }

        return SITE_MAP + DOT + XML;
    }

    String buildSiteMapUrl(Resource siteMapRoot, int index);

    int getIndex(SlingHttpServletRequest request);

}
