package cn.ilikui.learning;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Demonstrates equality contracts for HashMap keys. */
public final class HashMapLab {
    private HashMapLab() { }

    public static final class EmployeeKey {
        private final String employeeId;

        public EmployeeKey(String employeeId) {
            this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        }

        public String employeeId() { return employeeId; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof EmployeeKey that)) return false;
            return employeeId.equals(that.employeeId);
        }

        @Override
        public int hashCode() { return employeeId.hashCode(); }
    }

    public static Map<EmployeeKey, String> example() {
        Map<EmployeeKey, String> employees = new HashMap<>();
        employees.put(new EmployeeKey("E-100"), "Alice");
        employees.put(new EmployeeKey("E-100"), "Updated Alice");
        return employees;
    }

    public static void main(String[] args) {
        Map<EmployeeKey, String> employees = example();
        System.out.println("条目数：" + employees.size());
        System.out.println("按等价键查询：" + employees.get(new EmployeeKey("E-100")));
    }
}