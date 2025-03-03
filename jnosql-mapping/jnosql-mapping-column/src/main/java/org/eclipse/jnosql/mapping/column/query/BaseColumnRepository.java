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
package org.eclipse.jnosql.mapping.column.query;


import jakarta.data.page.Page;
import jakarta.data.page.Pageable;
import org.eclipse.jnosql.communication.Params;
import org.eclipse.jnosql.communication.column.ColumnDeleteQuery;
import org.eclipse.jnosql.communication.column.ColumnDeleteQueryParams;
import org.eclipse.jnosql.communication.column.ColumnObserverParser;
import org.eclipse.jnosql.communication.column.ColumnQuery;
import org.eclipse.jnosql.communication.column.ColumnQueryParams;
import org.eclipse.jnosql.communication.column.DeleteQueryParser;
import org.eclipse.jnosql.communication.column.SelectQueryParser;
import org.eclipse.jnosql.communication.query.DeleteQuery;
import org.eclipse.jnosql.communication.query.SelectQuery;
import org.eclipse.jnosql.communication.query.method.DeleteMethodProvider;
import org.eclipse.jnosql.communication.query.method.SelectMethodProvider;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.NoSQLPage;
import org.eclipse.jnosql.mapping.column.JNoSQLColumnTemplate;
import org.eclipse.jnosql.mapping.core.query.AbstractRepositoryProxy;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.core.repository.DynamicReturn;
import org.eclipse.jnosql.mapping.core.util.ParamsBinder;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A base abstract class for implementing column-oriented repositories in a Java NoSQL database.
 * This class provides common functionality for executing CRUD operations using column queries.
 *
 * @param <T> The type of entities managed by the repository.
 *
 */
public abstract class BaseColumnRepository<T, K> extends AbstractRepositoryProxy<T, K> {

    private static final SelectQueryParser SELECT_PARSER = new SelectQueryParser();

    private static final DeleteQueryParser DELETE_PARSER = new DeleteQueryParser();
    private static final Object[] EMPTY_PARAM = new Object[0];

    /**
     * Retrieves the Converters instance responsible for converting data types.
     *
     * @return The Converters instance.
     */
    protected abstract Converters converters();

    /**
     * Retrieves the metadata information about the entity managed by the repository.
     *
     * @return The EntityMetadata instance.
     */
    protected abstract EntityMetadata entityMetadata();

    /**
     * Retrieves the JNoSQLColumnTemplate instance for executing column queries.
     *
     * @return The JNoSQLColumnTemplate instance.
     */
    protected abstract JNoSQLColumnTemplate template();

    private ColumnObserverParser parser;

    private ParamsBinder paramsBinder;


    protected ColumnQuery query(Method method, Object[] args) {
        SelectMethodProvider provider = SelectMethodProvider.INSTANCE;
        SelectQuery selectQuery = provider.apply(method, entityMetadata().name());
        ColumnQueryParams queryParams = SELECT_PARSER.apply(selectQuery, parser());
        ColumnQuery query = queryParams.query();
        Params params = queryParams.params();
        paramsBinder().bind(params, args(args), method);
        return updateQueryDynamically(args(args), query);
    }

    private static Object[] args(Object[] args) {
        return args == null ? EMPTY_PARAM : args;
    }

    protected ColumnDeleteQuery deleteQuery(Method method, Object[] args) {
        DeleteMethodProvider deleteMethodFactory = DeleteMethodProvider.INSTANCE;
        DeleteQuery deleteQuery = deleteMethodFactory.apply(method, entityMetadata().name());
        ColumnDeleteQueryParams queryParams = DELETE_PARSER.apply(deleteQuery, parser());
        ColumnDeleteQuery query = queryParams.query();
        Params params = queryParams.params();
        paramsBinder().bind(params, args(args), method);
        return query;
    }

    /**
     * Retrieves the ColumnObserverParser instance for parsing column observations.
     *
     * @return The ColumnObserverParser instance.
     */

    protected ColumnObserverParser parser() {
        if (parser == null) {
            this.parser = new RepositoryColumnObserverParser(entityMetadata());
        }
        return parser;
    }

    /**
     * Retrieves the ParamsBinder instance for binding parameters to queries.
     *
     * @return The ParamsBinder instance.
     */
    protected ParamsBinder paramsBinder() {
        if (Objects.isNull(paramsBinder)) {
            this.paramsBinder = new ParamsBinder(entityMetadata(), converters());
        }
        return paramsBinder;
    }

    protected Object executeFindByQuery(Method method, Object[] args, Class<?> typeClass, ColumnQuery query) {
        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(typeClass)
                .withMethodSource(method)
                .withResult(() -> template().select(query))
                .withSingleResult(() -> template().singleResult(query))
                .withPagination(DynamicReturn.findPageable(args))
                .withStreamPagination(streamPagination(query))
                .withSingleResultPagination(getSingleResult(query))
                .withPage(getPage(query))
                .build();
        return dynamicReturn.execute();
    }

    protected Long executeCountByQuery(ColumnQuery query) {
        return template().count(query);
    }

    protected boolean executeExistsByQuery(ColumnQuery query) {
        return template().exists(query);
    }


    protected Function<Pageable, Page<T>> getPage(ColumnQuery query) {
        return p -> {
            Stream<T> entities = template().select(query);
            return NoSQLPage.of(entities.toList(), p);
        };
    }

    protected Function<Pageable, Optional<T>> getSingleResult(ColumnQuery query) {
        return p -> template().singleResult(query);
    }

    protected Function<Pageable, Stream<T>> streamPagination(ColumnQuery query) {
        return p -> template().select(query);
    }


    protected ColumnQuery updateQueryDynamically(Object[] args, ColumnQuery query) {
        DynamicQuery dynamicQuery = DynamicQuery.of(args, query);
        return dynamicQuery.get();
    }


}
