package org.dooq.tests;

public class Child {
    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Child setName(String name) {
        this.name = name;
        return this;
    }

    public Child setAge(int age) {
        this.age = age;
        return this;
    }
}
