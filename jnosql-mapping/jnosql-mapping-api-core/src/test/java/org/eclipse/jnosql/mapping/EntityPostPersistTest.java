package org.eclipse.jnosql.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityPostPersistTest {

    @Test
    public void testGet() {
        Object value = new Object();
        EntityPostPersist entity = new EntityPostPersist(value);
        assertEquals(value, entity.get());
    }

    @Test
    public void testEqualsAndHashCode() {
        Object value1 = new Object();
        Object value2 = new Object();

        EntityPostPersist entity1 = new EntityPostPersist(value1);
        EntityPostPersist entity2 = new EntityPostPersist(value1);
        EntityPostPersist entity3 = new EntityPostPersist(value2);

        assertEquals(entity1, entity1);
        assertEquals(entity1, entity2);
        assertEquals(entity2, entity1);
        assertEquals(entity1, entity2);
        assertNotEquals(entity1, null);
    }

    @Test
    public void testToString() {
        Object value = new Object();
        EntityPostPersist entity = new EntityPostPersist(value);
        String expected = "EntityPostPersist{value=" + value + "}";
        assertEquals(expected, entity.toString());
    }

    @Test
    public void testOf() {
        Object value = new Object();
        EntityPostPersist entity = EntityPostPersist.of(value);
        assertEquals(value, entity.get());
    }

    @Test
    public void testOfWithNullValue() {
        assertThrows(NullPointerException.class, () -> EntityPostPersist.of(null));
    }
}
