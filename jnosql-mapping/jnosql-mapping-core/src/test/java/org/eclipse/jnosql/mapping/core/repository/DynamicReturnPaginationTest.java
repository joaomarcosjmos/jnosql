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
package org.eclipse.jnosql.mapping.core.repository;

import jakarta.data.repository.CrudRepository;
import jakarta.data.page.Page;
import jakarta.data.page.Pageable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.concurrent.ThreadLocalRandom.current;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicReturnPaginationTest {

    @Mock
    private Function<Pageable, Stream<Person>> streamPagination;

    @Mock
    private Function<Pageable, Optional<Person>> singlePagination;

    @Mock
    private Function<Pageable, Page<Person>> page;

    @Test
    void shouldReturnEmptyOptional() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getOptional");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);

        Pageable pagination = getPagination();

        when(singlePagination.apply(pagination)).thenReturn(Optional.empty());

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page).build();
        Object execute = dynamicReturn.execute();

        Assertions.assertTrue(execute instanceof Optional);
        Optional<Person> optional = (Optional) execute;
        Assertions.assertFalse(optional.isPresent());

        Mockito.verify(singlePagination).apply(pagination);
        Mockito.verify(streamPagination, Mockito.never()).apply(pagination);
    }


    @Test
    void shouldReturnOptional() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getOptional");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();

        when(singlePagination.apply(pagination)).thenReturn(Optional.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page).build();

        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof Optional);
        Optional<Person> optional = (Optional) execute;
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(new Person("Ada"), optional.get());
        Mockito.verify(singlePagination).apply(pagination);
        Mockito.verify(streamPagination, Mockito.never()).apply(pagination);
    }


    @Test
    void shouldReturnAnInstance() throws NoSuchMethodException {
        Method method = getMethod(PersonRepository.class, "getInstance");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);

        Pageable pagination = getPagination();
        when(singlePagination.apply(pagination)).thenReturn(Optional.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page).build();

        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof Person);
        Person person = (Person) execute;
        Assertions.assertEquals(new Person("Ada"), person);
        Mockito.verify(singlePagination).apply(pagination);
        Mockito.verify(streamPagination, Mockito.never()).apply(pagination);
    }

    @Test
    void shouldReturnNull() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getInstance");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);

        Pageable pagination = getPagination();
        when(singlePagination.apply(pagination)).thenReturn(Optional.empty());

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();

        Object execute = dynamicReturn.execute();
        Assertions.assertNull(execute);

        Mockito.verify(singlePagination).apply(pagination);
        Mockito.verify(streamPagination, Mockito.never()).apply(pagination);
    }

    @Test
    void shouldReturnList() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getList");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();
        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof List);
        List<Person> persons = (List) execute;
        Assertions.assertFalse(persons.isEmpty());
        Assertions.assertEquals(new Person("Ada"), persons.get(0));

        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }

    @Test
    void shouldReturnIterable() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getIterable");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();

        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof Iterable);
        Iterable<Person> persons = (List) execute;
        Assertions.assertEquals(new Person("Ada"), persons.iterator().next());
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }

    @Test
    void shouldReturnCollection() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getCollection");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();
        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof Collection);
        Collection<Person> persons = (Collection) execute;
        Assertions.assertFalse(persons.isEmpty());
        Assertions.assertEquals(new Person("Ada"), persons.iterator().next());
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }

    @Test
    void shouldReturnSet() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getSet");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();
        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof Set);
        Set<Person> persons = (Set) execute;
        Assertions.assertFalse(persons.isEmpty());
        Assertions.assertEquals(new Person("Ada"), persons.iterator().next());
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }

    @Test
    void shouldReturnQueue() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getQueue");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();
        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof Queue);
        Queue<Person> persons = (Queue) execute;
        Assertions.assertFalse(persons.isEmpty());
        Assertions.assertEquals(new Person("Ada"), persons.iterator().next());
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }


    @Test
    void shouldReturnStream() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getStream");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();
        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof Stream);
        Stream<Person> persons = (Stream) execute;
        Assertions.assertEquals(new Person("Ada"), persons.iterator().next());
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }

    @Test
    void shouldReturnSortedSet() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getSortedSet");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();
        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof SortedSet);
        SortedSet<Person> persons = (SortedSet) execute;
        Assertions.assertFalse(persons.isEmpty());
        Assertions.assertEquals(new Person("Ada"), persons.iterator().next());
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }

    @Test
    void shouldReturnNavigableSet() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getNavigableSet");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();
        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof NavigableSet);
        NavigableSet<Person> persons = (NavigableSet) execute;
        Assertions.assertFalse(persons.isEmpty());
        Assertions.assertEquals(new Person("Ada"), persons.iterator().next());
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }


    @Test
    void shouldReturnDeque() throws NoSuchMethodException {

        Method method = getMethod(PersonRepository.class, "getDeque");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        when(streamPagination.apply(pagination)).thenReturn(Stream.of(new Person("Ada")));

        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();
        Object execute = dynamicReturn.execute();
        Assertions.assertTrue(execute instanceof Deque);
        Deque<Person> persons = (Deque) execute;
        Assertions.assertFalse(persons.isEmpty());
        Assertions.assertEquals(new Person("Ada"), persons.iterator().next());
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination).apply(pagination);
    }

    @Test
    void shouldReturnErrorWhenExecutePage() throws NoSuchMethodException {
        Method method = getMethod(PersonRepository.class, "getPage");
        Supplier<Stream<?>> stream = Stream::empty;
        Supplier<Optional<?>> singleResult = DynamicReturn.toSingleResult(method).apply(stream);
        Pageable pagination = getPagination();
        DynamicReturn<?> dynamicReturn = DynamicReturn.builder()
                .withClassSource(Person.class)
                .withMethodSource(method).withResult(stream)
                .withSingleResult(singleResult)
                .withPagination(pagination)
                .withStreamPagination(streamPagination)
                .withSingleResultPagination(singlePagination)
                .withPage(page)
                .build();

        dynamicReturn.execute();
        Mockito.verify(singlePagination, Mockito.never()).apply(pagination);
        Mockito.verify(streamPagination, Mockito.never()).apply(pagination);
        Mockito.verify(page).apply(pagination);
    }

    private Method getMethod(Class<?> repository, String methodName) throws NoSuchMethodException {
        return Stream.of(repository.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst().get();

    }

    private record Person(String name) implements Comparable<Person> {
        @Override
        public int compareTo(Person o) {
            return name.compareTo(o.name);
        }
    }


    private interface PersonRepository extends CrudRepository<Person, String> {

        Optional<Person> getOptional();

        Person getInstance();

        List<Person> getList();

        Iterable<Person> getIterable();

        Collection<Person> getCollection();

        Set<Person> getSet();

        Queue<Person> getQueue();

        Stream<Person> getStream();

        SortedSet<Person> getSortedSet();

        NavigableSet<Person> getNavigableSet();

        Deque<Person> getDeque();

        Page<Person> getPage();
    }

    private long getRandomLong() {
        return current().nextLong(1, 10);
    }

    private Pageable getPagination() {
        return Pageable.ofPage(getRandomLong()).size((int) getRandomLong());
    }

}