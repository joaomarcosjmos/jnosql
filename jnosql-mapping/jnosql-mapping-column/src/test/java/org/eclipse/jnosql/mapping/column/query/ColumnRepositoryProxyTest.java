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

import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.PageableRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.Sort;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.inject.Inject;
import jakarta.nosql.PreparedStatement;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.column.Column;
import org.eclipse.jnosql.communication.column.ColumnCondition;
import org.eclipse.jnosql.communication.column.ColumnDeleteQuery;
import org.eclipse.jnosql.communication.column.ColumnQuery;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.column.ColumnEntityConverter;
import org.eclipse.jnosql.mapping.column.JNoSQLColumnTemplate;
import org.eclipse.jnosql.mapping.column.MockProducer;
import org.eclipse.jnosql.mapping.column.entities.Person;
import org.eclipse.jnosql.mapping.column.entities.PersonStatisticRepository;
import org.eclipse.jnosql.mapping.column.entities.Vendor;
import org.eclipse.jnosql.mapping.column.spi.ColumnExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.Condition.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnableAutoWeld
@AddPackages(value = {Converters.class, ColumnEntityConverter.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({EntityMetadataExtension.class, ColumnExtension.class})
class ColumnRepositoryProxyTest {

    private JNoSQLColumnTemplate template;

    @Inject
    private EntitiesMetadata entities;

    @Inject
    private Converters converters;

    private PersonRepository personRepository;

    private VendorRepository vendorRepository;


    @BeforeEach
    void setUp() {
        this.template = Mockito.mock(JNoSQLColumnTemplate.class);

        ColumnRepositoryProxy personHandler = new ColumnRepositoryProxy(template,
                entities, PersonRepository.class, converters);

        ColumnRepositoryProxy vendorHandler = new ColumnRepositoryProxy(template,
                entities, VendorRepository.class, converters);

        when(template.insert(any(Person.class))).thenReturn(Person.builder().build());
        when(template.insert(any(Person.class), any(Duration.class))).thenReturn(Person.builder().build());
        when(template.update(any(Person.class))).thenReturn(Person.builder().build());

        personRepository = (PersonRepository) Proxy.newProxyInstance(PersonRepository.class.getClassLoader(),
                new Class[]{PersonRepository.class},
                personHandler);
        vendorRepository = (VendorRepository) Proxy.newProxyInstance(VendorRepository.class.getClassLoader(),
                new Class[]{VendorRepository.class}, vendorHandler);
    }


    @Test
    void shouldSaveUsingInsertWhenDataDoesNotExist() {
        when(template.find(Person.class, 10L)).thenReturn(Optional.empty());

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        Person person = Person.builder().withName("Ada")
                .withId(10L)
                .withPhones(singletonList("123123"))
                .build();
        assertNotNull(personRepository.save(person));
        verify(template).insert(captor.capture());
        Person value = captor.getValue();
        assertEquals(person, value);
    }


    @Test
    void shouldSaveUsingUpdateWhenDataExists() {

        when(template.find(Person.class, 10L)).thenReturn(Optional.of(Person.builder().build()));

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        Person person = Person.builder().withName("Ada")
                .withId(10L)
                .withPhones(singletonList("123123"))
                .build();
        assertNotNull(personRepository.save(person));
        verify(template).update(captor.capture());
        Person value = captor.getValue();
        assertEquals(person, value);
    }


    @Test
    void shouldSaveIterable() {
        when(personRepository.findById(10L)).thenReturn(Optional.empty());

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        Person person = Person.builder().withName("Ada")
                .withId(10L)
                .withPhones(singletonList("123123"))
                .build();

        personRepository.saveAll(singletonList(person));
        verify(template).insert(captor.capture());
        Person personCapture = captor.getValue();
        assertEquals(person, personCapture);
    }


    @Test
    void shouldFindByNameInstance() {

        when(template.singleResult(any(ColumnQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        personRepository.findByName("name");

        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).singleResult(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(Condition.EQUALS, condition.condition());
        assertEquals(Column.of("name", "name"), condition.column());

        assertNotNull(personRepository.findByName("name"));
        when(template.singleResult(any(ColumnQuery.class))).thenReturn(Optional
                .empty());

        assertNull(personRepository.findByName("name"));


    }

    @Test
    void shouldFindByNameANDAge() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        List<Person> persons = personRepository.findByNameAndAge("name", 20);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        assertThat(persons).contains(ada);

    }

    @Test
    void shouldFindByAgeANDName() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        Set<Person> persons = personRepository.findByAgeAndName(20, "name");
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        assertThat(persons).contains(ada);

    }

    @Test
    void shouldFindByNameANDAgeOrderByName() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        Stream<Person> persons = personRepository.findByNameAndAgeOrderByName("name", 20);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        assertThat(persons.collect(Collectors.toList())).contains(ada);

    }

    @Test
    void shouldFindByNameANDAgeOrderByAge() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        Queue<Person> persons = personRepository.findByNameAndAgeOrderByAge("name", 20);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        assertThat(persons).contains(ada);

    }

    @Test
    void shouldDeleteByName() {
        ArgumentCaptor<ColumnDeleteQuery> captor = ArgumentCaptor.forClass(ColumnDeleteQuery.class);
        personRepository.deleteByName("Ada");
        verify(template).delete(captor.capture());
        ColumnDeleteQuery deleteQuery = captor.getValue();
        ColumnCondition condition = deleteQuery.condition().get();
        assertEquals("Person", deleteQuery.name());
        assertEquals(Condition.EQUALS, condition.condition());
        assertEquals(Column.of("name", "Ada"), condition.column());

    }

    @Test
    void shouldFindById() {
        personRepository.findById(10L);
        verify(template).find(Person.class, 10L);
    }

    @Test
    void shouldFindByIds() {
        when(template.find(Mockito.eq(Person.class), Mockito.any(Long.class)))
                .thenReturn(Optional.of(Person.builder().build()));

        personRepository.findByIdIn(singletonList(10L)).toList();
        verify(template).find(Person.class, 10L);

        personRepository.findByIdIn(asList(1L, 2L, 3L)).toList();
        verify(template, times(4)).find(Mockito.eq(Person.class), Mockito.any(Long.class));
    }

    @Test
    void shouldDeleteById() {
        ArgumentCaptor<ColumnDeleteQuery> captor = ArgumentCaptor.forClass(ColumnDeleteQuery.class);
        personRepository.deleteById(10L);
        verify(template).delete(Person.class, 10L);
    }

    @Test
    void shouldDeleteByIds() {
        ArgumentCaptor<ColumnDeleteQuery> captor = ArgumentCaptor.forClass(ColumnDeleteQuery.class);
        personRepository.deleteByIdIn(singletonList(10L));
        verify(template).delete(Person.class, 10L);
    }


    @Test
    void shouldContainsById() {
        when(template.find(Person.class, 10L)).thenReturn(Optional.of(Person.builder().build()));

        assertTrue(personRepository.existsById(10L));
        Mockito.verify(template).find(Person.class, 10L);

        when(template.find(Person.class, 10L)).thenReturn(Optional.empty());
        assertFalse(personRepository.existsById(10L));

    }

    @Test
    void shouldFindAll() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findAll().toList();
        ArgumentCaptor<Class<?>> captor = ArgumentCaptor.forClass(Class.class);
        verify(template).findAll(captor.capture());
        assertEquals(captor.getValue(), Person.class);

    }

    @Test
    void shouldDeleteAll() {
        personRepository.deleteAll();
        ArgumentCaptor<Class<?>> captor = ArgumentCaptor.forClass(Class.class);
        verify(template).deleteAll(captor.capture());
        assertEquals(captor.getValue(), Person.class);

    }

    @Test
    void shouldDeleteEntity(){
        Person person = Person.builder().withId(1L).withAge(20).withName("Ada").build();
        personRepository.delete(person);
        verify(template).delete(Person.class, 1L);
    }

    @Test
    void shouldDeleteEntities(){
        Person person = Person.builder().withId(1L).withAge(20).withName("Ada").build();
        personRepository.deleteAll(List.of(person));
        verify(template).delete(Person.class, 1L);
    }

    @Test
    void shouldReturnToString() {
        assertNotNull(personRepository.toString());
    }

    @Test
    void shouldReturnSameHashCode() {
        assertEquals(personRepository.hashCode(), personRepository.hashCode());
    }

    @Test
    void shouldReturnNotNull() {
        assertNotNull(personRepository);
    }

    @Test
    void shouldFindByNameAndAgeGreaterEqualThan() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByNameAndAgeGreaterThanEqual("Ada", 33);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(AND, condition.condition());
        List<ColumnCondition> conditions = condition.column().get(new TypeReference<>() {
        });
        ColumnCondition columnCondition = conditions.get(0);
        ColumnCondition columnCondition2 = conditions.get(1);

        assertEquals(Condition.EQUALS, columnCondition.condition());
        assertEquals("Ada", columnCondition.column().get());
        assertEquals("name", columnCondition.column().name());

        assertEquals(Condition.GREATER_EQUALS_THAN, columnCondition2.condition());
        assertEquals(33, columnCondition2.column().get());
        assertEquals("age", columnCondition2.column().name());
    }

    @Test
    void shouldFindByGreaterThan() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByAgeGreaterThan(33);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(GREATER_THAN, condition.condition());
        assertEquals(Column.of("age", 33), condition.column());

    }

    @Test
    void shouldFindByAgeLessThanEqual() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByAgeLessThanEqual(33);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(LESSER_EQUALS_THAN, condition.condition());
        assertEquals(Column.of("age", 33), condition.column());

    }

    @Test
    void shouldFindByAgeLessEqual() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByAgeLessThan(33);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(LESSER_THAN, condition.condition());
        assertEquals(Column.of("age", 33), condition.column());

    }

    @Test
    void shouldFindByAgeBetween() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByAgeBetween(10, 15);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(BETWEEN, condition.condition());
        List<Value> values = condition.column().get(new TypeReference<>() {
        });
        assertEquals(Arrays.asList(10, 15), values.stream().map(Value::get).collect(Collectors.toList()));
        assertTrue(condition.column().name().contains("age"));
    }


    @Test
    void shouldFindByNameLike() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByNameLike("Ada");
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(LIKE, condition.condition());
        assertEquals(Column.of("name", "Ada"), condition.column());

    }

    @Test
    void shouldGotOrderException() {
        Assertions.assertThrows(MappingException.class, () ->
                personRepository.findBy());
    }

    @Test
    void shouldGotOrderException2() {
        Assertions.assertThrows(MappingException.class, () ->
                personRepository.findByException());
    }


    @Test
    void shouldFindByStringWhenFieldIsSet() {
        Vendor vendor = new Vendor("vendor");
        vendor.setPrefixes(Collections.singleton("prefix"));

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(vendor));

        vendorRepository.findByPrefixes("prefix");

        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).singleResult(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("vendors", query.name());
        assertEquals(EQUALS, condition.condition());
        assertEquals(Column.of("prefixes", "prefix"), condition.column());

    }

    @Test
    void shouldFindByIn() {
        Vendor vendor = new Vendor("vendor");
        vendor.setPrefixes(Collections.singleton("prefix"));

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(vendor));

        vendorRepository.findByPrefixesIn(singletonList("prefix"));

        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).singleResult(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("vendors", query.name());
        assertEquals(IN, condition.condition());

    }


    @Test
    void shouldConvertFieldToTheType() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByAge("120");
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(EQUALS, condition.condition());
        assertEquals(Column.of("age", 120), condition.column());
    }

    @Test
    void shouldFindByActiveTrue() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByActiveTrue();
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(EQUALS, condition.condition());
        assertEquals(Column.of("active", true), condition.column());
    }

    @Test
    void shouldFindByActiveFalse() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findByActiveFalse();
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(EQUALS, condition.condition());
        assertEquals(Column.of("active", false), condition.column());
    }


    @Test
    void shouldExecuteJNoSQLQuery() {
        personRepository.findByQuery();
        verify(template).query("select * from Person");
    }

    @Test
    void shouldExecuteJNoSQLPrepare() {
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        when(template.prepare(Mockito.anyString())).thenReturn(statement);
        personRepository.findByQuery("Ada");
        verify(statement).bind("id", "Ada");
    }

    @Test
    void shouldFindBySalary_Currency() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findBySalary_Currency("USD");
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        final Column column = condition.column();
        assertEquals("Person", query.name());
        assertEquals("salary.currency", column.name());

    }

    @Test
    void shouldFindBySalary_CurrencyAndSalary_Value() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();
        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));
        personRepository.findBySalary_CurrencyAndSalary_Value("USD", BigDecimal.TEN);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        final Column column = condition.column();
        final List<ColumnCondition> conditions = column.get(new TypeReference<>() {
        });
        final List<String> names = conditions.stream().map(ColumnCondition::column)
                .map(Column::name).collect(Collectors.toList());
        assertEquals("Person", query.name());
        assertThat(names).contains("salary.currency", "salary.value");

    }

    @Test
    void shouldFindBySalary_CurrencyOrderByCurrency_Name() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(ColumnQuery.class)))
                .thenReturn(Stream.of(ada));

        personRepository.findBySalary_CurrencyOrderByCurrency_Name("USD");
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        final Sort sort = query.sorts().get(0);
        final Column document = condition.column();
        assertEquals("Person", query.name());
        assertEquals("salary.currency", document.name());
        assertEquals("currency.name", sort.property());
    }

    @Test
    void shouldCountByName() {
        when(template.count(any(ColumnQuery.class)))
                .thenReturn(10L);

        var result = personRepository.countByName("Poliana");
        Assertions.assertEquals(10L, result);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).count(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        final Column column = condition.column();
        assertEquals("Person", query.name());
        assertEquals("name", column.name());
        assertEquals("Poliana", column.get());
    }

    @Test
    void shouldExistsByName() {
        when(template.exists(any(ColumnQuery.class)))
                .thenReturn(true);

        var result = personRepository.existsByName("Poliana");
        assertTrue(result);
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).exists(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        final Column column = condition.column();
        assertEquals("Person", query.name());
        assertEquals("name", column.name());
        assertEquals("Poliana", column.get());
    }

    @Test
    void shouldFindByNameNot() {

        when(template.singleResult(any(ColumnQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        personRepository.findByNameNot("name");

        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).singleResult(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals(Condition.NOT, condition.condition());
        assertEquals("Person", query.name());
        ColumnCondition columnCondition = condition.column().get(ColumnCondition.class);
        assertEquals(Condition.EQUALS, columnCondition.condition());
        assertEquals(Column.of("name", "name"), columnCondition.column());

        assertNotNull(personRepository.findByName("name"));
        when(template.singleResult(any(ColumnQuery.class))).thenReturn(Optional
                .empty());

        assertNull(personRepository.findByName("name"));
    }

    @Test
    void shouldFindByNameNotEquals() {

        when(template.singleResult(any(ColumnQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        personRepository.findByNameNotEquals("name");

        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).singleResult(captor.capture());
        ColumnQuery query = captor.getValue();
        ColumnCondition condition = query.condition().get();
        assertEquals(Condition.NOT, condition.condition());
        assertEquals("Person", query.name());
        ColumnCondition columnCondition = condition.column().get(ColumnCondition.class);
        assertEquals(Condition.EQUALS, columnCondition.condition());
        assertEquals(Column.of("name", "name"), columnCondition.column());

        assertNotNull(personRepository.findByName("name"));
        when(template.singleResult(any(ColumnQuery.class))).thenReturn(Optional
                .empty());

        assertNull(personRepository.findByName("name"));
    }

    @Test
    void shouldExecuteDefaultMethod() {
        personRepository.partcionate("name");

        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template, Mockito.times(2)).singleResult(captor.capture());
        List<ColumnQuery> values = captor.getAllValues();
        assertThat(values).isNotNull().hasSize(2);
    }

    @Test
    void shouldUseQueriesFromOtherInterface() {
        personRepository.findByNameLessThan("name");

        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        assertEquals("Person", query.name());
        ColumnCondition condition = query.condition().get();
        assertEquals(LESSER_THAN, condition.condition());
        assertEquals(Column.of("name", "name"), condition.column());
    }

    @Test
    void shouldUseDefaultMethodFromOtherInterface() {
        personRepository.ada();

        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        assertEquals("Person", query.name());
        ColumnCondition condition = query.condition().get();
        assertEquals(LESSER_THAN, condition.condition());
        assertEquals(Column.of("name", "Ada"), condition.column());
    }

    @Test
    void shouldExecuteCustomRepository(){
        PersonStatisticRepository.PersonStatistic statistics = personRepository.statistics("Salvador");
        assertThat(statistics).isNotNull();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(statistics.average()).isEqualTo(26);
            softly.assertThat(statistics.sum()).isEqualTo(26);
            softly.assertThat(statistics.max()).isEqualTo(26);
            softly.assertThat(statistics.min()).isEqualTo(26);
            softly.assertThat(statistics.count()).isEqualTo(1);
            softly.assertThat(statistics.city()).isEqualTo("Salvador");
        });
    }

    @Test
    void shouldInsertUsingAnnotation(){
        Person person = Person.builder().withName("Ada")
                .withId(10L)
                .withPhones(singletonList("123123"))
                .build();
        personRepository.insertPerson(person);
        Mockito.verify(template).insert(person);
    }

    @Test
    void shouldUpdateUsingAnnotation(){
        Person person = Person.builder().withName("Ada")
                .withId(10L)
                .withPhones(singletonList("123123"))
                .build();
        personRepository.updatePerson(person);
        Mockito.verify(template).update(person);
    }

    @Test
    void shouldDeleteUsingAnnotation(){
        Person person = Person.builder().withName("Ada")
                .withId(10L)
                .withPhones(singletonList("123123"))
                .build();
        personRepository.deletePerson(person);
        Mockito.verify(template).delete(Person.class, 10L);
    }

    @Test
    void shouldSaveUsingAnnotation(){
        Person person = Person.builder().withName("Ada")
                .withId(10L)
                .withPhones(singletonList("123123"))
                .build();
        personRepository.savePerson(person);
        Mockito.verify(template).insert(person);
    }

    @Test
    void shouldExecuteMatchParameter(){
        personRepository.find("Ada");
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        verify(template).select(captor.capture());
        ColumnQuery query = captor.getValue();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(query.name()).isEqualTo("Person");
            var condition = query.condition().orElseThrow();
            softly.assertThat(condition.condition()).isEqualTo(Condition.EQUALS);
            softly.assertThat(condition.column()).isEqualTo(Column.of("name", "Ada"));
        });
    }

    public interface BaseQuery<T> {

        List<T> findByNameLessThan(String name);

        default List<T> ada() {
            return this.findByNameLessThan("Ada");
        }
    }

    public interface PersonRepository extends PageableRepository<Person, Long>, BaseQuery<Person>, PersonStatisticRepository {

        List<Person> findByActiveTrue();

        List<Person> findByActiveFalse();

        List<Person> findBySalary_Currency(String currency);

        List<Person> findBySalary_CurrencyAndSalary_Value(String currency, BigDecimal value);

        List<Person> findBySalary_CurrencyOrderByCurrency_Name(String currency);

        Person findByName(String name);

        Person findByNameNot(String name);

        Person findByNameNotEquals(String name);

        @Insert
        Person insertPerson(Person person);
        @Update
        Person updatePerson(Person person);

        @Save
        Person savePerson(Person person);

        @Delete
        void deletePerson(Person person);

        default Map<Boolean, List<Person>> partcionate(String name) {
            Objects.requireNonNull(name, "name is required");

            var person = Person.builder()
                    .withName("Ada Lovelace")
                    .withAge(20)
                    .withId(1L).build();
            findByName(name);
            findByNameNot(name);
            Map<Boolean, List<Person>> map = new HashMap<>();
            map.put(true, List.of(person));
            map.put(false, List.of(person));
            return map;
        }

        void deleteByName(String name);

        List<Person> findByAge(String age);

        List<Person> findByNameAndAge(String name, Integer age);

        Set<Person> findByAgeAndName(Integer age, String name);

        Stream<Person> findByNameAndAgeOrderByName(String name, Integer age);

        Queue<Person> findByNameAndAgeOrderByAge(String name, Integer age);

        Set<Person> findByNameAndAgeGreaterThanEqual(String name, Integer age);

        Set<Person> findByAgeGreaterThan(Integer age);

        Set<Person> findByAgeLessThanEqual(Integer age);

        Set<Person> findByAgeLessThan(Integer age);

        Set<Person> findByAgeBetween(Integer ageA, Integer ageB);

        Set<Person> findByNameLike(String name);

        @Query("select * from Person")
        Optional<Person> findByQuery();

        @Query("select * from Person where id = @id")
        Optional<Person> findByQuery(@Param("id") String id);

        long countByName(String name);

        boolean existsByName(String name);

        @OrderBy("name")
        List<Person> findBy();

        @OrderBy("name")
        @OrderBy("age")
        List<Person> findByException();

        List<Person> find(@By("name") String name);
    }

    public interface VendorRepository extends PageableRepository<Vendor, String> {

        Vendor findByPrefixes(String prefix);

        Vendor findByPrefixesIn(List<String> prefix);

    }
}
