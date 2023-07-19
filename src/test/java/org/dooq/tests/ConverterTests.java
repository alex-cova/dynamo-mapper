package org.dooq.tests;

import org.dooq.converter.DynamoConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class ConverterTests {

    @Test
    void writeAndRead() {

        var pojo = new Pojo()
                .setAge(33)
                .setName("Alex")
                .setFlags(List.of(true))
                .setSex(true)
                .setHobbies(List.of("football", "basketball"))
                .setScores(List.of(1, 2, 3))
                .setMap(Map.of("key", new java.math.BigDecimal("1.2")));



        var map = DynamoConverter.getConverter(Pojo.class).write(pojo);

        System.out.println(map);

        Assertions.assertEquals(Pojo.class.getDeclaredFields().length, map.size());

        var result = DynamoConverter.getConverter(Pojo.class).read(map);

        Assertions.assertEquals(pojo, result);

    }
}
