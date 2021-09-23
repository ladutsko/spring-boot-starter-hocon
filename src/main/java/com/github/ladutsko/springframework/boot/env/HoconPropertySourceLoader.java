/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 George Ladutsko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.ladutsko.springframework.boot.env;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

/**
 * Strategy to load HOCON files into a {@link PropertySource}.
 *
 * @author <a href="mailto:ladutsko@gmail.com">George Ladutsko</a>
 * @see YamlPropertySourceLoader
 */
public class HoconPropertySourceLoader implements PropertySourceLoader {

    /**
     * Returns the file extensions that the loader supports (excluding the '.').
     *
     * @return the file extensions
     */
    @Override
    public String[] getFileExtensions() {
        return new String[] { "conf" };
    }

    /**
     * Load the resource into one or more property sources. Implementations may either
     * return a list containing a single source, or in the case of a multi-document format
     * such as yaml a source for each document in the resource.
     *
     * @param name     the root name of the property source. If multiple documents are loaded
     *                 an additional suffix should be added to the name for each source loaded.
     * @param resource the resource to load
     * @return a list property sources
     * @throws IOException if the source cannot be loaded
     */
    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        
        if (!ClassUtils.isPresent("com.typesafe.config.Config", getClass().getClassLoader())) {
            throw new IllegalStateException(
                "Attempted to load " + name + " but com.typesafe:config was not found on the classpath");
        }

        final Config config = ConfigFactory.parseURL(resource.getURL()).resolve();

        final Map<String, Object> result = buildFlattenedMap(config.root().unwrapped());
        if (result.isEmpty()) {
            return emptyList();
        }

        return singletonList(new MapPropertySource(name, result));
    }

    private Map<String, Object> buildFlattenedMap(final Map<String, Object> root) {

        final Map<String, Object> result = new LinkedHashMap<>();

        root.forEach((prop, value) -> buildFlattenedMap(result, prop, value));

        return result;
    }

    private void buildFlattenedMap(
        final Map<String, Object> result,
        final String path,
        final Object value
    ) {

        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> object = (Map<String, Object>) value;
            object.forEach((k, v) ->
                buildFlattenedMap(result, String.join(".", path, k), v)
            );
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>) value;
            for (int idx = 0; idx < list.size(); idx++) {
                buildFlattenedMap(result, path + "[" + idx + "]", list.get(idx));
            }
        } else {
            result.put(path, value);
        }
    }
}
