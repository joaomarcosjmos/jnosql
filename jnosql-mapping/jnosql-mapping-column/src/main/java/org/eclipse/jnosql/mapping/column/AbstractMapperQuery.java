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
package org.eclipse.jnosql.mapping.column;

import org.eclipse.jnosql.communication.column.Column;
import org.eclipse.jnosql.communication.column.ColumnCondition;
import org.eclipse.jnosql.mapping.Converters;
import org.eclipse.jnosql.mapping.reflection.EntityMetadata;
import org.eclipse.jnosql.mapping.util.ConverterUtil;

import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

abstract class AbstractMapperQuery {

    protected final String columnFamily;

    protected boolean negate;

    protected ColumnCondition condition;

    protected boolean and;

    protected String name;

    protected transient final EntityMetadata mapping;

    protected transient final Converters converters;

    protected transient final JNoSQLColumnTemplate template;

    protected long start;

    protected long limit;


    AbstractMapperQuery(EntityMetadata mapping, Converters converters, JNoSQLColumnTemplate template) {
        this.mapping = mapping;
        this.converters = converters;
        this.columnFamily = mapping.name();
        this.template = template;
    }

    protected void appendCondition(ColumnCondition incomingCondition) {
        ColumnCondition columnCondition = getColumnCondition(incomingCondition);
        
        if (Objects.nonNull(condition)) {
            this.condition = and ? condition.and(columnCondition) : condition.or(columnCondition);
        } else {
            this.condition = columnCondition;
        }
        
        negate = false;
        name = null;
    }

    protected <T> void betweenImpl(T valueA, T valueB) {
        requireNonNull(valueA, "valueA is required");
        requireNonNull(valueB, "valueB is required");
        ColumnCondition newCondition = ColumnCondition
                .between(Column.of(mapping.columnField(name), asList(getValue(valueA), getValue(valueB))));
        appendCondition(newCondition);
    }


    protected <T> void inImpl(Iterable<T> values) {

        requireNonNull(values, "values is required");
        List<Object> convertedValues = StreamSupport.stream(values.spliterator(), false)
                .map(this::getValue).collect(toList());
        ColumnCondition newCondition = ColumnCondition
                .in(Column.of(mapping.columnField(name), convertedValues));
        appendCondition(newCondition);
    }

    protected <T> void eqImpl(T value) {
        requireNonNull(value, "value is required");

        ColumnCondition newCondition = ColumnCondition
                .eq(Column.of(mapping.columnField(name), getValue(value)));
        appendCondition(newCondition);
    }

    protected void likeImpl(String value) {
        requireNonNull(value, "value is required");
        ColumnCondition newCondition = ColumnCondition
                .like(Column.of(mapping.columnField(name), getValue(value)));
        appendCondition(newCondition);
    }

    protected <T> void gteImpl(T value) {
        requireNonNull(value, "value is required");
        ColumnCondition newCondition = ColumnCondition
                .gte(Column.of(mapping.columnField(name), getValue(value)));
        appendCondition(newCondition);
    }

    protected <T> void gtImpl(T value) {
        requireNonNull(value, "value is required");
        ColumnCondition newCondition = ColumnCondition
                .gt(Column.of(mapping.columnField(name), getValue(value)));
        appendCondition(newCondition);
    }

    protected <T> void ltImpl(T value) {
        requireNonNull(value, "value is required");
        ColumnCondition newCondition = ColumnCondition
                .lt(Column.of(mapping.columnField(name), getValue(value)));
        appendCondition(newCondition);
    }

    protected <T> void lteImpl(T value) {
        requireNonNull(value, "value is required");
        ColumnCondition newCondition = ColumnCondition
                .lte(Column.of(mapping.columnField(name), getValue(value)));
        appendCondition(newCondition);
    }

    protected Object getValue(Object value) {
        return ConverterUtil.getValue(value, mapping, name, converters);
    }

    private ColumnCondition getColumnCondition(ColumnCondition newCondition) {
        if (negate) {
            return newCondition.negate();
        } else {
            return newCondition;
        }
    }
}
