# dynamo-mapper

Generates converter classes (Java 17) for POJOs at runtime, using ASM.

## Features

* Fast ⚡️
* Easy to use
* No reflection used at conversion time
* Little memory footprint
* Easy to add additional converters
* Support for java records

## Benchmark

```
Benchmark                          Mode  Cnt     Score   Error  Units
ConverterBenchmark.readBenchmark   avgt    5   635.997 ± 1.419  ns/op
ConverterBenchmark.writeBenchmark  avgt    5  1071.437 ± 4.250  ns/op
```

### Requirements

* Target class must have a default constructor
* Target class must have getters and setters for all fields to parse

if you want to omit some fields, you can use `@DynamoIgnore` annotation or `transient` keyword on field,
**this doesn't apply to records.**

## Usage:

```java
public Map<String, AttributeValue> writeToMap(Pojo pojo){
        return DynamoConverter.getConverter(Pojo.class).write(pojo);
        }
```

```java
public Pojo readFromMap(Map<String, AttributeValue> map){
        return DynamoConverter.getConverter(Pojo.class).read(map);
        }
```

---

## Implementing custom converters

You can implement custom converters by creating an abstract class that extends `ObjectParser`

```java
public abstract class CustomObjectConverter<T> extends ObjectParser<T> {

    @Override
    protected AttributeValue writeString(String value) {
        return AttributeValue.fromS("custom");
    }

    protected AttributeValue writeCustom(Custom custom) {
        return AttributeValue.fromS(custom.getCustomValue());
    }

    protected Custom readCustom(AttributeValue value) {
        return new Custom(value.s());
    }
}
```

**Requirements:**

- Class must be abstract
- Class must have a default constructor
- Read and Write methods must exist, have only one parameter and must be `protected`
- Name of the converter must be unique, and not named `ObjectParser` because the generated class is named `'TargetName' + 'ConverterName'`

```java
public Map<String, AttributeValue> writeToMap(Pojo pojo){
        return DynamoConverter.getConverter(Pojo.class,CustomObjectConverter.class)
        .write(pojo);
        }
```

---

## How this works

Target class:

```java

public class Pojo {
    private String name;
    private int age;
    private boolean sex;
    private List<String> hobbies;
    private List<Integer> scores;
    private List<Boolean> flags;
    private Map<String, BigDecimal> map;

    //getters and setters omitted
}
```

Generated parser class:

```java
import java.math.BigDecimal;
import java.util.Map;

import org.dooq.tests.Pojo;

public class PojoParser extends ObjectParser {
    public Pojo newInstance() {
        return new Pojo();
    }

    public PojoParser() {
    }

    public Pojo read(Map<String, AttributeValue> var1) {
        Pojo var2 = new Pojo();
        var2.setName(this.parseString((software.amazon.awssdk.services.dynamodb.model.AttributeValue) var1.get("name")));
        var2.setAge(this.parseInt((software.amazon.awssdk.services.dynamodb.model.AttributeValue) var1.get("age")));
        var2.setSex(this.parseBool((software.amazon.awssdk.services.dynamodb.model.AttributeValue) var1.get("sex")));
        var2.setHobbies(this.parseStringList((software.amazon.awssdk.services.dynamodb.model.AttributeValue) var1.get("hobbies")));
        var2.setScores(this.parseList((software.amazon.awssdk.services.dynamodb.model.AttributeValue) var1.get("scores"), Integer.class));
        var2.setFlags(this.parseList((software.amazon.awssdk.services.dynamodb.model.AttributeValue) var1.get("flags"), Boolean.class));
        var2.setMap(this.parseMap((software.amazon.awssdk.services.dynamodb.model.AttributeValue) var1.get("map"), BigDecimal.class));
        return var2;
    }

    public Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> write(Pojo var1) {
        FilteredMap var2 = new FilteredMap();
        var2.put("name", this.writeString(var1.getName()));
        var2.put("age", this.writeInt(var1.getAge()));
        var2.put("sex", this.writeBool(var1.isSex()));
        var2.put("hobbies", this.writeStringList(var1.getHobbies()));
        var2.put("scores", this.writeList(var1.getScores(), Integer.class));
        var2.put("flags", this.writeList(var1.getFlags(), Boolean.class));
        var2.put("map", this.writeMap(var1.getMap(), BigDecimal.class));
        return var2;
    }
}
```

Byte code of generated class:

```
{
  public org.dooq.tests.Pojo newInstance();
    descriptor: ()Ljava/lang/Object;
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=2, locals=1, args_size=1
         0: new           #9                  // class org/dooq/tests/Pojo
         3: dup
         4: invokespecial #13                 // Method org/dooq/tests/Pojo."<init>":()V
         7: areturn
    Signature: #7                           // ()Lorg/dooq/tests/Pojo;

  public org.dooq.converter.PojoParser();
    descriptor: ()V
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #14                 // Method org/dooq/converter/ObjectParser."<init>":()V
         4: return

  public org.dooq.tests.Pojo read(java.util.Map<java.lang.String, AttributeValue>);
    descriptor: (Ljava/util/Map;)Ljava/lang/Object;
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=15, locals=3, args_size=2
         0: new           #9                  // class org/dooq/tests/Pojo
         3: dup
         4: invokespecial #13                 // Method org/dooq/tests/Pojo."<init>":()V
         7: astore_2
         8: aload_2
         9: aload_0
        10: aload_1
        11: ldc           #19                 // String name
        13: invokeinterface #25,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
        18: checkcast     #27                 // class software/amazon/awssdk/services/dynamodb/model/AttributeValue
        21: invokevirtual #31                 // Method org/dooq/converter/ObjectParser.parseString:(Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;)Ljava/lang/String;
        24: invokevirtual #35                 // Method org/dooq/tests/Pojo.setName:(Ljava/lang/String;)Lorg/dooq/tests/Pojo;
        27: pop
        28: aload_2
        29: aload_0
        30: aload_1
        31: ldc           #37                 // String age
        33: invokeinterface #25,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
        38: checkcast     #27                 // class software/amazon/awssdk/services/dynamodb/model/AttributeValue
        41: invokevirtual #41                 // Method org/dooq/converter/ObjectParser.parseInt:(Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;)I
        44: invokevirtual #45                 // Method org/dooq/tests/Pojo.setAge:(I)Lorg/dooq/tests/Pojo;
        47: pop
        48: aload_2
        49: aload_0
        50: aload_1
        51: ldc           #47                 // String sex
        53: invokeinterface #25,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
        58: checkcast     #27                 // class software/amazon/awssdk/services/dynamodb/model/AttributeValue
        61: invokevirtual #51                 // Method org/dooq/converter/ObjectParser.parseBool:(Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;)Z
        64: invokevirtual #55                 // Method org/dooq/tests/Pojo.setSex:(Z)Lorg/dooq/tests/Pojo;
        67: pop
        68: aload_2
        69: aload_0
        70: aload_1
        71: ldc           #57                 // String hobbies
        73: invokeinterface #25,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
        78: checkcast     #27                 // class software/amazon/awssdk/services/dynamodb/model/AttributeValue
        81: invokevirtual #61                 // Method org/dooq/converter/ObjectParser.parseStringList:(Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;)Ljava/util/List;
        84: invokevirtual #65                 // Method org/dooq/tests/Pojo.setHobbies:(Ljava/util/List;)Lorg/dooq/tests/Pojo;
        87: pop
        88: aload_2
        89: aload_0
        90: aload_1
        91: ldc           #67                 // String scores
        93: invokeinterface #25,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
        98: checkcast     #27                 // class software/amazon/awssdk/services/dynamodb/model/AttributeValue
       101: ldc           #69                 // class java/lang/Integer
       103: invokevirtual #73                 // Method org/dooq/converter/ObjectParser.parseList:(Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;Ljava/lang/Class;)Ljava/util/List;
       106: invokevirtual #76                 // Method org/dooq/tests/Pojo.setScores:(Ljava/util/List;)Lorg/dooq/tests/Pojo;
       109: pop
       110: aload_2
       111: aload_0
       112: aload_1
       113: ldc           #78                 // String flags
       115: invokeinterface #25,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
       120: checkcast     #27                 // class software/amazon/awssdk/services/dynamodb/model/AttributeValue
       123: ldc           #80                 // class java/lang/Boolean
       125: invokevirtual #73                 // Method org/dooq/converter/ObjectParser.parseList:(Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;Ljava/lang/Class;)Ljava/util/List;
       128: invokevirtual #83                 // Method org/dooq/tests/Pojo.setFlags:(Ljava/util/List;)Lorg/dooq/tests/Pojo;
       131: pop
       132: aload_2
       133: aload_0
       134: aload_1
       135: ldc           #85                 // String map
       137: invokeinterface #25,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
       142: checkcast     #27                 // class software/amazon/awssdk/services/dynamodb/model/AttributeValue
       145: ldc           #87                 // class java/math/BigDecimal
       147: invokevirtual #91                 // Method org/dooq/converter/ObjectParser.parseMap:(Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;Ljava/lang/Class;)Ljava/util/Map;
       150: invokevirtual #95                 // Method org/dooq/tests/Pojo.setMap:(Ljava/util/Map;)Lorg/dooq/tests/Pojo;
       153: pop
       154: aload_2
       155: areturn
    Signature: #17                          // (Ljava/util/Map<Ljava/lang/String;LAttributeValue;>;)Lorg/dooq/tests/Pojo;

  public java.util.Map<java.lang.String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> write(org.dooq.tests.Pojo);
    descriptor: (Lorg/dooq/tests/Pojo;)Ljava/util/Map;
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=15, locals=3, args_size=2
         0: new           #100                // class org/dooq/converter/FilteredMap
         3: dup
         4: invokespecial #101                // Method org/dooq/converter/FilteredMap."<init>":()V
         7: astore_2
         8: aload_2
         9: ldc           #19                 // String name
        11: aload_0
        12: aload_1
        13: invokevirtual #105                // Method org/dooq/tests/Pojo.getName:()Ljava/lang/String;
        16: invokevirtual #109                // Method org/dooq/converter/ObjectParser.writeString:(Ljava/lang/String;)Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;
        19: invokeinterface #113,  3          // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        24: pop
        25: aload_2
        26: ldc           #37                 // String age
        28: aload_0
        29: aload_1
        30: invokevirtual #117                // Method org/dooq/tests/Pojo.getAge:()I
        33: invokevirtual #121                // Method org/dooq/converter/ObjectParser.writeInt:(I)Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;
        36: invokeinterface #113,  3          // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        41: pop
        42: aload_2
        43: ldc           #47                 // String sex
        45: aload_0
        46: aload_1
        47: invokevirtual #125                // Method org/dooq/tests/Pojo.isSex:()Z
        50: invokevirtual #129                // Method org/dooq/converter/ObjectParser.writeBool:(Z)Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;
        53: invokeinterface #113,  3          // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        58: pop
        59: aload_2
        60: ldc           #57                 // String hobbies
        62: aload_0
        63: aload_1
        64: invokevirtual #133                // Method org/dooq/tests/Pojo.getHobbies:()Ljava/util/List;
        67: invokevirtual #137                // Method org/dooq/converter/ObjectParser.writeStringList:(Ljava/util/List;)Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;
        70: invokeinterface #113,  3          // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        75: pop
        76: aload_2
        77: ldc           #67                 // String scores
        79: aload_0
        80: aload_1
        81: invokevirtual #140                // Method org/dooq/tests/Pojo.getScores:()Ljava/util/List;
        84: ldc           #69                 // class java/lang/Integer
        86: invokevirtual #144                // Method org/dooq/converter/ObjectParser.writeList:(Ljava/util/List;Ljava/lang/Class;)Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;
        89: invokeinterface #113,  3          // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        94: pop
        95: aload_2
        96: ldc           #78                 // String flags
        98: aload_0
        99: aload_1
       100: invokevirtual #147                // Method org/dooq/tests/Pojo.getFlags:()Ljava/util/List;
       103: ldc           #80                 // class java/lang/Boolean
       105: invokevirtual #144                // Method org/dooq/converter/ObjectParser.writeList:(Ljava/util/List;Ljava/lang/Class;)Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;
       108: invokeinterface #113,  3          // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
       113: pop
       114: aload_2
       115: ldc           #85                 // String map
       117: aload_0
       118: aload_1
       119: invokevirtual #151                // Method org/dooq/tests/Pojo.getMap:()Ljava/util/Map;
       122: ldc           #87                 // class java/math/BigDecimal
       124: invokevirtual #155                // Method org/dooq/converter/ObjectParser.writeMap:(Ljava/util/Map;Ljava/lang/Class;)Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;
       127: invokeinterface #113,  3          // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
       132: pop
       133: aload_2
       134: areturn
    Signature: #98                          // (Lorg/dooq/tests/Pojo;)Ljava/util/Map<Ljava/lang/String;Lsoftware/amazon/awssdk/services/dynamodb/model/AttributeValue;>;

  public java.util.Map write(java.lang.Object);
    descriptor: (Ljava/lang/Object;)Ljava/util/Map;
    flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
    Code:
      stack=2, locals=2, args_size=2
         0: aload_0
         1: aload_1
         2: checkcast     #9                  // class org/dooq/tests/Pojo
         5: invokevirtual #158                // Method write:(Lorg/dooq/tests/Pojo;)Ljava/util/Map;
         8: areturn
}

```
