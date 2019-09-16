package com.peregrine.commons.test.mock;

import static com.peregrine.commons.util.PerConstants.SLASH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class ResourceMock extends ResourceWrapper {

    protected static final String DEFAULT_NAME = null;

    protected final Resource mock;
    protected final Node node;

    protected final Map<String, Object> properties = new HashMap<>();

    private final Map<String, Resource> children = new LinkedHashMap<>();

    private final Map<Class, Object> adaptTo = new LinkedHashMap<>();

    private ResourceResolver resourceResolver;

    public ResourceMock(final String name) {
        super(mock(Resource.class, name));
        mock = getResource();
        node = mock(Node.class, concatenateToDerivedName(name, " Node"));
        final ValueMap valueMap = new ValueMapDecorator(properties);
        when(mock.getValueMap()).thenReturn(valueMap);
        addAdapter(node);
    }

    protected static String concatenateToDerivedName(final String... strings) {
        final StringBuilder builder = new StringBuilder();
        for (final String s : strings) {
            if (s == null) {
                return DEFAULT_NAME;
            }

            builder.append(s);
        }

        return StringUtils.trim(builder.toString());
    }

    public ResourceMock() {
        this(DEFAULT_NAME);
    }

    public final ResourceMock setResourceResolver(final ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
        when(mock.getResourceResolver()).thenReturn(resourceResolver);
        updateResourceResolverGetResource();
        return this;
    }

    private void updateResourceResolverGetResource() {
        if (resourceResolver != null) {
            when(resourceResolver.getResource(getPath())).thenReturn(this);
        }
    }

    public final Map<String, Object> getProperties() {
        return properties;
    }

    public final ResourceMock putProperty(final String name, final Object property) {
        properties.put(name, property);
        return this;
    }

    public Object getProperty(final String name) {
        return properties.get(name);
    }

    public final ResourceMock setPath(final String path) {
        when(mock.getPath()).thenReturn(path);
        try {
            when(node.getPath()).thenReturn(path);
        } catch (final RepositoryException e) { }
        updateResourceResolverGetResource();
        setPathImpl(path);
        return this;
    }

    protected void setPathImpl(final String path) { }

    @Override
    public final String getName() {
        return StringUtils.substringAfterLast(getPath(), SLASH);
    }

    public final ResourceMock setParent(final Resource parent) {
        when(mock.getParent()).thenReturn(parent);
        return this;
    }

    @Override
    public final Resource getChild(final String name) {
        return children.get(name);
    }

    public final ResourceMock addChild(final String name, final Resource child) {
        children.put(name, child);
        return this;
    }

    public final ResourceMock addChild(final Resource child) {
        addChild(child.getName(), child);
        return this;
    }

    @Override
    public Iterable<Resource> getChildren() {
        return children.values();
    }

    @Override
    public Iterator<Resource> listChildren() {
        return getChildren().iterator();
    }

    @Override
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void addAdapter(final Object adapter) {
        adaptTo.put(adapter.getClass(), adapter);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if (adaptTo.containsKey(type)) {
            return (AdapterType) adaptTo.get(type);
        }

        for (final Map.Entry<Class, Object> entry : adaptTo.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return (AdapterType) entry.getValue();
            }
        }

        return super.adaptTo(type);
    }

    public Node getNode() {
        return node;
    }
}
