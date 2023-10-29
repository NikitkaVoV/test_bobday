package ru.nikitavov.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NewDepartment(@JsonProperty("department_name") String departmentName, List<NewEmployer> employees) {
}
