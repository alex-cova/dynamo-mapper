package org.dooq.tests;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Pojo {
    private String name;
    private int age;
    private boolean sex;
    private List<String> hobbies;
    private List<Integer> scores;
    private List<Boolean> flags;
    private Map<String, BigDecimal> map;

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public boolean isSex() {
        return sex;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public List<Integer> getScores() {
        return scores;
    }

    public List<Boolean> getFlags() {
        return flags;
    }

    public Map<String, BigDecimal> getMap() {
        return map;
    }

    public Pojo setName(String name) {
        this.name = name;
        return this;
    }

    public Pojo setAge(int age) {
        this.age = age;
        return this;
    }

    public Pojo setSex(boolean sex) {
        this.sex = sex;
        return this;
    }

    public Pojo setHobbies(List<String> hobbies) {
        this.hobbies = hobbies;
        return this;
    }

    public Pojo setScores(List<Integer> scores) {
        this.scores = scores;
        return this;
    }

    public Pojo setFlags(List<Boolean> flags) {
        this.flags = flags;
        return this;
    }

    public Pojo setMap(Map<String, BigDecimal> map) {
        this.map = map;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pojo pojo = (Pojo) o;

        if (age != pojo.age) return false;
        if (sex != pojo.sex) return false;

        if (!name.equals(pojo.name)) {
            System.out.println("name not equal %s vs %s".formatted(name, pojo.name));
            return false;
        }

        if (!hobbies.equals(pojo.hobbies)) {
            System.out.println("hobbies not equal %s vs %s".formatted(hobbies, pojo.hobbies));
            return false;
        }

        if (!scores.equals(pojo.scores)) {
            System.out.println("scores not equal %s vs %s".formatted(scores, pojo.scores));
            return false;
        }
        if (!flags.equals(pojo.flags)) {
            System.out.println("flags not equal %s vs %s".formatted(flags, pojo.flags));
            return false;
        }

        if (!map.equals(pojo.map)) {
            System.out.println("map not equal %s vs %s".formatted(map, pojo.map));
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + age;
        result = 31 * result + (sex ? 1 : 0);
        result = 31 * result + hobbies.hashCode();
        result = 31 * result + scores.hashCode();
        result = 31 * result + flags.hashCode();
        result = 31 * result + map.hashCode();
        return result;
    }
}
