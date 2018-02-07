/*
 *
 *  Copyright (c) 2017 Otávio Santana and others
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
 *
 */
package org.jnosql.diana.api.document.query;

/**
 * The base to name condition
 */
public interface DocumentNameCondition {


    /**
     * Creates the equals condition {@link org.jnosql.diana.api.Condition#EQUALS}
     *
     * @param value the value to the condition
     * @param <T>   the type
     * @return the {@link DocumentWhere}
     * @throws NullPointerException when value is null
     */
    <T> DocumentWhere eq(T value);

    /**
     * Creates the like condition {@link org.jnosql.diana.api.Condition#LIKE}
     *
     * @param value the value to the condition
     * @return the {@link DocumentWhere}
     * @throws NullPointerException when value is null
     */
    DocumentWhere like(String value);

    /**
     * Creates the greater than condition {@link org.jnosql.diana.api.Condition#GREATER_THAN}
     *
     * @param value the value to the condition
     * @return the {@link DocumentWhere}
     * @throws NullPointerException when value is null
     */
    DocumentWhere gt(Number value);

    /**
     * Creates the greater equals than condition {@link org.jnosql.diana.api.Condition#GREATER_EQUALS_THAN}
     *
     * @param value the value to the condition
     * @return the {@link DocumentWhere}
     * @throws NullPointerException when value is null
     */
    DocumentWhere gte(Number value);

    /**
     * Creates the lesser than condition {@link org.jnosql.diana.api.Condition#LESSER_THAN}
     *
     * @param value the value to the condition
     * @return the {@link DocumentWhere}
     * @throws NullPointerException when value is null
     */
    DocumentWhere lt(Number value);

    /**
     * Creates the lesser equals than condition {@link org.jnosql.diana.api.Condition#LESSER_EQUALS_THAN}
     *
     * @param value the value to the condition
     * @return the {@link DocumentWhere}
     * @throws NullPointerException when value is null
     */
    DocumentWhere lte(Number value);

    /**
     * Creates the between condition {@link org.jnosql.diana.api.Condition#EQUALS}
     *
     * @param valueA the values within a given range
     * @param valueB the values within a given range
     * @return the {@link DocumentWhere}
     * @throws NullPointerException when either valueA or valueB are null
     */
    DocumentWhere between(Number valueA, Number valueB);

    /**
     * Creates in condition {@link org.jnosql.diana.api.Condition#IN}
     * @param values the values
     * @param <T> the type
     * @return the {@link DocumentWhere}
     * @throws NullPointerException when value is null
     */
    <T> DocumentWhere    in(Iterable<T> values);
}
