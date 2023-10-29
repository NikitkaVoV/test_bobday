package ru.nikitavov.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OldDepartment(Integer id, @JsonProperty("department_name") String departmentName) {
}
