package com.example.validatingforminput;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public class PersonForm {
    @NotNull
    @Min(18)
    private Integer age;

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String toString() {
        return "Person(Age: " + this.age + ")";
    }
}
