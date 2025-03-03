/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.mapping.graph.query;

import jakarta.data.repository.PageableRepository;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.query.AbstractRepository;
import org.eclipse.jnosql.mapping.graph.GraphConverter;
import org.eclipse.jnosql.mapping.graph.GraphTemplate;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;

import java.lang.reflect.ParameterizedType;

/**
 * Proxy handle to generate {@link jakarta.data.repository.PageableRepository}
 *
 * @param <T>  the type
 * @param <K> the K type
 */
class GraphRepositoryProxy<T, K> extends AbstractGraphRepositoryProxy<T, K> {


    private final GraphRepository repository;

    private final EntityMetadata entityMetadata;

    private final Graph graph;

    private final GraphConverter converter;

    private final GraphTemplate template;

    private final Converters converters;

    private final Class<?> repositoryType;


    GraphRepositoryProxy(GraphTemplate template, EntitiesMetadata entities,
                         Class<?> repositoryType,
                         Graph graph, GraphConverter converter,
                         Converters converters) {

        Class<T> typeClass = (Class) ((ParameterizedType) repositoryType.getGenericInterfaces()[0])
                .getActualTypeArguments()[0];

        this.graph = graph;
        this.converter = converter;
        this.entityMetadata = entities.get(typeClass);
        this.repository = new GraphRepository(template, entityMetadata);
        this.template = template;
        this.converters = converters;
        this.repositoryType = repositoryType;

    }

    @Override
    protected EntityMetadata entityMetadata() {
        return entityMetadata;
    }

    @Override
    protected AbstractRepository<T, K> repository() {
        return repository;
    }

    @Override
    protected Graph graph() {
        return graph;
    }

    @Override
    protected GraphConverter converter() {
        return converter;
    }

    @Override
    protected GraphTemplate template() {
        return template;
    }

    @Override
    protected Converters converters() {
        return converters;
    }

    @Override
    protected Class<?> repositoryType() {
        return repositoryType;
    }


    static class GraphRepository extends AbstractGraphRepository implements PageableRepository {

        private final GraphTemplate template;

        private final EntityMetadata entityMetadata;

        GraphRepository(GraphTemplate template, EntityMetadata entityMetadata) {
            this.template = template;
            this.entityMetadata = entityMetadata;
        }

        @Override
        protected GraphTemplate template() {
            return template;
        }

        @Override
        protected EntityMetadata entityMetadata() {
            return entityMetadata;
        }

    }
}
