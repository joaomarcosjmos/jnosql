/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.mapping.graph;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.mapping.AttributeConverter;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.ConstructorBuilder;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.metadata.ParameterMetaData;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * Given a {@link Vertex} it will create an entity from the database information using the constructor.
 * It might be a record or a class that provides constructor annotations
 *
 * @param <T> the entity type
 */
final class EntityConverterByContructor<T> implements Supplier<T> {

    private final EntityMetadata mapping;
    private final Vertex vertex;

    private final Converters converters;

    private EntityConverterByContructor(EntityMetadata mapping, Vertex vertex, Converters converters) {
        this.mapping = mapping;
        this.vertex = vertex;
        this.converters = converters;
    }

    @Override
    public T get() {
        ConstructorBuilder builder = ConstructorBuilder.of(mapping.constructor());
        List<Property<?>> properties = vertex.keys().stream()
                .map(k -> DefaultProperty.of(k, vertex.value(k)))
                .collect(toList());
        for (ParameterMetaData parameter : builder.parameters()) {

            if (parameter.isId()) {
                feedId(builder, parameter);
            } else {
                feedRegularFeilds(builder, properties, parameter);
            }
        }
        return builder.build();
    }

    private void feedId(ConstructorBuilder builder, ParameterMetaData parameter) {
        Object vertexId = vertex.id();
        if (Objects.nonNull(vertexId)) {
            parameter.converter().ifPresentOrElse(c -> {
                AttributeConverter<Object, Object> attributeConverter = this.converters.get(parameter);
                Object attributeConverted = attributeConverter.convertToEntityAttribute(vertexId);
                Value value = Value.of(attributeConverted);
                builder.add(value.get(parameter.type()));
            }, () -> builder.add(Value.of(vertexId).get(parameter.type())));
        } else {
            builder.addEmptyParameter();
        }
    }

    private void feedRegularFeilds(ConstructorBuilder builder, List<Property<?>> properties, ParameterMetaData parameter) {
        Optional<Property<?>> property = properties.stream()
                .filter(c -> c.key().equals(parameter.name()))
                .findFirst();
        property.ifPresentOrElse(p -> parameter.converter().ifPresentOrElse(c -> {
            Object value = this.converters.get(parameter).convertToEntityAttribute(p.value());
            builder.add(value);
        }, () -> {
            Value value = Value.of(p.value());
            builder.add(value.get(parameter.type()));
        }), builder::addEmptyParameter);
    }

    static <T> EntityConverterByContructor<T> of(EntityMetadata mapping, Vertex vertex, Converters converters) {
        return new EntityConverterByContructor<>(mapping, vertex, converters);
    }
}