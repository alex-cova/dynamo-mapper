package org.dooq.tests;

import org.dooq.converter.Converter;
import org.dooq.converter.DynamoConverter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConverterBenchmark {

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(ConverterBenchmark.class.getSimpleName())
                .threads(4)
                .forks(1)
                .build();

        new Runner(opt).run();
    }


    static Converter<Pojo> converter = DynamoConverter.getConverter(Pojo.class);

    static Pojo pojo = new Pojo()
            .setAge(33)
            .setName("Alex")
            .setFlags(List.of(true))
            .setSex(true)
            .setHobbies(List.of("football", "basketball"))
            .setScores(List.of(1, 2, 3))
            .setMap(Map.of("key", new java.math.BigDecimal("1.2")));

    static Map<String, AttributeValue> map = converter.write(pojo);

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @BenchmarkMode(Mode.AverageTime)
    public void writeBenchmark() {
        converter.write(pojo);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @BenchmarkMode(Mode.AverageTime)
    public void readBenchmark() {
        converter.read(map);
    }
}
