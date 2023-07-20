package org.dooq.tests;

import org.dooq.converter.Converter;
import org.dooq.converter.DynamoConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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


        Map<String, AttributeValue> map = DynamoConverter.getConverter(Pojo.class).write(pojo);

        System.out.println(map);

        Assertions.assertEquals(Pojo.class.getDeclaredFields().length, map.size());

        Pojo result = DynamoConverter.getConverter(Pojo.class).read(map);

        Assertions.assertEquals(pojo, result);

    }

    @Test
    void writeAndReadRecord() {

        var recordExample = new RecordExample("Alex", 33, true, BigDecimal.TEN);//Yes 10 dollars :C

        var converter = DynamoConverter.getConverter(RecordExample.class);

        var map = converter.write(recordExample);

        System.out.println(map);

        var resultRecord = converter.read(map);

        Assertions.assertEquals(recordExample, resultRecord);
    }




}
