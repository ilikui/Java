package cn.ilikui.learning;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HashMapLabTest {
    @Test
    void equalEmployeeKeysReplaceSameEntry() {
        Map<HashMapLab.EmployeeKey, String> employees = HashMapLab.example();
        assertEquals(1, employees.size());
        assertEquals("Updated Alice", employees.get(new HashMapLab.EmployeeKey("E-100")));
    }

    @Test
    void employeeKeyEqualityIsConsistentWithHashCode() {
        var first = new HashMapLab.EmployeeKey("E-200");
        var second = new HashMapLab.EmployeeKey("E-200");
        var third = new HashMapLab.EmployeeKey("E-201");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void employeeIdMustNotBeNull() {
        assertThrows(NullPointerException.class, () -> new HashMapLab.EmployeeKey(null));
    }
}