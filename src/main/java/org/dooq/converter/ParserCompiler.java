package org.dooq.converter;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.beans.Transient;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

/**
 * DynamoDB reflection-less high performance record parser compiler
 *
 * @author alex
 */
@ApiStatus.Experimental
class ParserCompiler extends ClassLoader {

    public static boolean DEBUG = System.getProperty("dooq.converter.debug", "false").equalsIgnoreCase("true");

    private static final ParserCompiler INSTANCE = new ParserCompiler();
    private final Map<Class<?>, ConverterStruct> converterMap = new HashMap<>();

    private ParserCompiler() {
        converterMap.put(ObjectParser.class, new ConverterStruct(ObjectParser.class));
    }

    private Class<?> defineNewClass(byte[] bytecode, String name) {

        if (DEBUG) {
            try {

                var file = new File("compiled");
                var ignored = file.mkdir();

                FileOutputStream outputStream = new FileOutputStream(new File(file, name + ".class"));
                outputStream.write(bytecode);
                outputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return super.defineClass("org.dooq.converter." + name, bytecode, 0, bytecode.length);
    }

    public static <T> @NotNull ObjectParser<T> getConverter(@NotNull Class<T> type) {
        return getConverter(type, ObjectParser.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> @NotNull ObjectParser<T> getConverter(@NotNull Class<T> type, Class<? extends ObjectParser> parserClass) {

        @Nullable var converter = INSTANCE.converterMap.get(parserClass);

        if (converter == null) {
            converter = new ConverterStruct(parserClass);
            INSTANCE.converterMap.put(parserClass, converter);
        }

        var parser = converter
                .getCachedParser(type);

        if (parser != null) {
            return (ObjectParser<T>) parser;
        }

        if (DEBUG) {
            Logger.getLogger(ParserCompiler.class.getName())
                    .log(Level.INFO, "Creating converter for class: " + type);
        }

        if (!type.isRecord()) {
            boolean invalidConstructor = true;

            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 0) {
                    invalidConstructor = false;
                    break;
                }
            }

            if (invalidConstructor) {
                throw new IllegalArgumentException("No args constructor is required for type '%s'".formatted(type));
            }
        }

        ClassWriter writer = new ClassWriter(0);

        writer.visit(V17, ACC_PUBLIC, getParentName(type, parserClass),
                null, Type.getInternalName(parserClass), null);

        defineNewInstance(writer, type);
        defineConstructor(writer, parserClass);
        defineReadMethod(writer, type, parserClass);
        defineWriteMethod(writer, type, parserClass);

        writer.visitEnd();

        byte[] bytecode = writer.toByteArray();

        var parserInstance = createObject(bytecode, type, parserClass);

        INSTANCE.converterMap.get(parserClass)
                .putCachedParser(type, parserInstance);

        return parserInstance;
    }

    private static void defineNewInstance(@NotNull ClassWriter writer, @NotNull Class<?> type) {

        // Define the parse method
        MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, "newInstance", "()Ljava/lang/Object;",
                "()L" + Type.getInternalName(type) + ";", null);

        visitor.visitCode();

        if (type.isRecord()) { //Return null

            visitor.visitInsn(Opcodes.ACONST_NULL);
            visitor.visitInsn(Opcodes.ARETURN);

            visitor.visitMaxs(1, 1);
            visitor.visitEnd();

            return;
        }

        visitor.visitTypeInsn(NEW, Type.getInternalName(type));
        visitor.visitInsn(DUP);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);

        visitor.visitInsn(Opcodes.ARETURN);
        visitor.visitMaxs(2, 1);
        visitor.visitEnd();

    }

    @SuppressWarnings("rawtypes")
    private static void defineWriteMethod(@NotNull ClassWriter writer, @NotNull Class<?> type, Class<? extends ObjectParser> parserClass) {

        var signature = "(L" + Type.getInternalName(type) + ";)Ljava/util/Map<Ljava/lang/String;L" + Type.getInternalName(AttributeValue.class) + ";>;";

        var descriptor = "(L" + Type.getInternalName(type) + ";)Ljava/util/Map;";

        // Define the parse method
        MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, "write", descriptor, signature, null);


        visitor.visitCode();

        visitor.visitTypeInsn(NEW, Type.getInternalName(FilteredMap.class));
        visitor.visitInsn(DUP);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(FilteredMap.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        visitor.visitVarInsn(ASTORE, 2);

        var stacks = generateWriteMethods(visitor, type, parserClass);

        visitor.visitMaxs(stacks + 1, 3);

        visitor.visitVarInsn(ALOAD, 2);
        visitor.visitInsn(Opcodes.ARETURN);

        visitor.visitEnd();


        // Write bridge

        visitor = writer.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC | ACC_BRIDGE, "write",
                "(Ljava/lang/Object;)Ljava/util/Map;", null, null);

        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(ALOAD, 1);
        visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(type));
        visitor.visitMethodInsn(INVOKEVIRTUAL, getParentName(type, parserClass), "write", descriptor, false);

        visitor.visitInsn(ARETURN);
        visitor.visitMaxs(2, 2);
        visitor.visitEnd();
    }

    @Contract(pure = true)
    private static <T> @NotNull String getParentName(@NotNull Class<T> type, Class<?> parserClass) {
        return "org/dooq/converter/" + type.getSimpleName() + parserClass.getSimpleName();
    }


    @SuppressWarnings("rawtypes")
    private static int generateWriteMethods(MethodVisitor visitor, @NotNull Class<?> type, Class<? extends ObjectParser> parserClass) {

        Map<String, Method> methodMap = new HashMap<>();

        for (Method method : type.getDeclaredMethods()) {

            if (method.isAnnotationPresent(Transient.class)) continue;
            if (method.getParameterCount() > 0) continue;

            if (methodMap.put(method.getName().toLowerCase(), method) != null) {
                throw new IllegalStateException("Duplicate method found: " + method.getName());
            }
        }

        if (methodMap.isEmpty()) throw new IllegalStateException("No accessors found for class " + type);

        int stacks = 0;

        String methodName;

        for (Field field : getFields(type)) {

            Method getMethod;

            methodName = field.getName().toLowerCase();

            if (type.isRecord()) {
                getMethod = methodMap.get(methodName);
            } else {
                getMethod = methodMap.get("get" + methodName);

                if (getMethod == null && field.getType() == boolean.class) {
                    getMethod = methodMap.get("is" + methodName);
                }
            }

            if (getMethod == null) {
                continue;
            }

            stacks += 2;

            computeWriter(visitor, field.getName(), field.getType(), getMethod, type, getGenericType(field), parserClass);
        }

        return stacks;
    }

    @Contract("_ -> new")
    private static @NotNull Parameters getGenericType(@NotNull Field field) {
        return getGenericType(field.getGenericType());
    }

    @Contract("_ -> new")
    static @NotNull Parameters getGenericType(@NotNull java.lang.reflect.Type type) {

        if (type instanceof Class<?> clazz) {
            return new Parameters(clazz);
        }

        var arguments = ((ParameterizedType) type)
                .getActualTypeArguments();

        if (arguments.length == 1) {
            java.lang.reflect.Type actualTypeArgument = arguments[0];

            if (actualTypeArgument instanceof Class<?> clazz) {
                return new Parameters(clazz);
            }
        } else if (arguments.length == 2) {

            java.lang.reflect.Type firstArgument = arguments[0];
            java.lang.reflect.Type secondArgument = arguments[1];

            if (firstArgument instanceof Class<?> clazz && secondArgument instanceof Class<?> clazz2) {
                return new Parameters(clazz, clazz2);
            }
        }

        throw new IllegalStateException("Cannot determine generic type: " + type);
    }

    record Parameters(Class<?> param1, @Nullable Class<?> param2) {
        public Parameters(Class<?> param1) {
            this(param1, null);
        }
    }

    /**
     * Finds the required writer method for the given field
     *
     * @param visitor    The method visitor
     * @param name       The field name
     * @param valueType  The field type
     * @param setMethod  The setter method
     * @param parentType The parent type
     * @param parameters The generic parameters
     */
    @SuppressWarnings("rawtypes")
    private static void computeWriter(MethodVisitor visitor, String name,
                                      @NotNull Class<?> valueType, @Nullable Method setMethod,
                                      Class<?> parentType, Parameters parameters,
                                      Class<? extends ObjectParser> parserClass) {


        @Nullable var method = getWriterMethod(valueType, parserClass);

        if (method != null) {
            handleWriterMethod(visitor, name, setMethod, method, parserClass);
            return;
        }


        if (valueType == List.class) {

            if (parameters.param1 == String.class) {
                handleWriterMethod(visitor, name, setMethod, getMethodByName("writeStringList", parserClass), parserClass);
                return;
            }

            handleGenericWriteMethod(visitor, name, setMethod, "writeList", parameters.param1, parserClass);
            return;

        }
        if (valueType == Set.class) {

            if (parameters.param1 == String.class) {
                handleWriterMethod(visitor, name, setMethod, getMethodByName("writeStringSet", parserClass), parserClass);
                return;
            }

            handleGenericWriteMethod(visitor, name, setMethod, "writeSet", parameters.param1, parserClass);
            return;
        }


        if (valueType == Map.class) {
            handleGenericWriteMethod(visitor, name, setMethod, "writeMap", parameters.param2, parserClass);
            return;
        }

        if (isCustomClass(valueType)) {
            handleGenericWriteMethod(visitor, name, setMethod, "writeComplex", valueType, parserClass);
            return;
        }

        if (isJVMClass(valueType)) {
            throw new RuntimeException("Not implemented: " + valueType + " in class " + parentType);
        }

        Logger.getLogger(ParserCompiler.class.getName())
                .log(Level.WARNING, "Not implemented: " + valueType + " in class " + parentType);

    }

    @SuppressWarnings("rawtypes")
    private static void computeReader(MethodVisitor visitor, String name,
                                      @NotNull Class<?> valueType, @Nullable Method setMethod,
                                      Class<?> parentType, Parameters parameters,
                                      Class<? extends ObjectParser> parserClass) {


        @Nullable var method = getReaderMethod(valueType, parserClass);

        if (method != null) {
            handleMethod(visitor, name, setMethod, method, parentType, parserClass);
            return;
        }

        if (valueType == List.class) {

            if (parameters.param1 == String.class) {
                handleMethod(visitor, name, setMethod, getMethodByName("parseStringList", parserClass), parentType, parserClass);
                return;
            }

            handleGenericSetMethod(visitor, name, setMethod, "parseList", parameters.param1, parentType, parserClass);
            return;

        }
        if (valueType == Set.class) {

            if (parameters.param1 == String.class) {
                handleMethod(visitor, name, setMethod, getMethodByName("parseStringSet", parserClass), parentType, parserClass);
                return;
            }

            handleGenericSetMethod(visitor, name, setMethod, "parseSet", parameters.param1, parentType, parserClass);
            return;
        }


        if (valueType == Map.class) {
            handleGenericSetMethod(visitor, name, setMethod, "parseMap", parameters.param2, parentType, parserClass);
            return;
        }


        if (isCustomClass(valueType)) {
            handleComplex(visitor, name, setMethod, valueType, parentType, parserClass);
            return;
        }

        if (isJVMClass(valueType)) {
            throw new RuntimeException("Not implemented: " + valueType + " in class " + parentType);
        }

        Logger.getLogger(ParserCompiler.class.getName())
                .log(Level.WARNING, "Not implemented: " + valueType + " in class " + parentType);
    }

    private static @NotNull Method getMethodByName(String name, Class<?> parserClass) {
        return INSTANCE.converterMap.get(parserClass).getMethod(name);
    }

    @SuppressWarnings("rawtypes")
    private static void handleWriterMethod(@NotNull MethodVisitor visitor, String name,
                                           @Nullable Method setMethod, @NotNull Method parser, @NotNull Class<? extends ObjectParser> parserClass) {

        Objects.requireNonNull(setMethod, "No setter found for field: " + name);

        visitor.visitVarInsn(ALOAD, 2);
        visitor.visitLdcInsn(name);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(ALOAD, 1);

        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(setMethod.getDeclaringClass()), setMethod.getName(), Type.getMethodDescriptor(setMethod), false);
        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(parserClass), parser.getName(), Type.getMethodDescriptor(parser), false);

        visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);

        visitor.visitInsn(POP);

    }

    @SuppressWarnings("rawtypes")
    private static void handleGenericWriteMethod(@NotNull MethodVisitor visitor, String name, @Nullable Method setMethod,
                                                 String parser, Class<?> type, Class<? extends ObjectParser> parserClass) {

        Objects.requireNonNull(setMethod, "No setter found for field: " + name);

        visitor.visitVarInsn(ALOAD, 2);
        visitor.visitLdcInsn(name);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(ALOAD, 1);

        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(setMethod.getDeclaringClass()), setMethod.getName(), Type.getMethodDescriptor(setMethod), false);

        visitor.visitLdcInsn(Type.getType(type));

        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ObjectParser.class), parser, Type.getMethodDescriptor(getMethodByName(parser, parserClass)), false);
        visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);

        visitor.visitInsn(POP);
    }

    @SuppressWarnings("rawtypes")
    private static void handleMethod(@NotNull MethodVisitor visitor, String name, @Nullable Method setMethod,
                                     Method parser, @NotNull Class<?> parent, Class<? extends ObjectParser> parserClass) {

        if (parent.isRecord()) {

            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitVarInsn(ALOAD, 1);
            visitor.visitLdcInsn(name);

            visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(AttributeValue.class));
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(parserClass), parser.getName(),
                    Type.getMethodDescriptor(parser), false);

            return;
        }

        Objects.requireNonNull(setMethod);

        visitor.visitVarInsn(ALOAD, 2);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(ALOAD, 1);

        visitor.visitLdcInsn(name); //Load the key onto the stack

        visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(AttributeValue.class));
        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(parserClass), parser.getName(),
                Type.getMethodDescriptor(parser), false);

        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(setMethod.getDeclaringClass()),
                setMethod.getName(), Type.getMethodDescriptor(setMethod), false);

        if (setMethod.getReturnType() != void.class) {
            visitor.visitInsn(POP);
        }

    }

    @SuppressWarnings("rawtypes")
    private static void handleGenericSetMethod(@NotNull MethodVisitor visitor, String name,
                                               @Nullable Method setMethod, String parser,
                                               Class<?> type, @NotNull Class<?> parent,
                                               Class<? extends ObjectParser> parserClass) {

        @Nullable var readerMethod = getMethodByName(parser, parserClass);

        Objects.requireNonNull(readerMethod, "No method found with name: " + parser);

        if (parent.isRecord()) {

            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitVarInsn(ALOAD, 1);
            visitor.visitLdcInsn(name);

            visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(AttributeValue.class));

            visitor.visitLdcInsn(Type.getType(type));

            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(parserClass), parser,
                    Type.getMethodDescriptor(readerMethod), false);

            return;
        }

        Objects.requireNonNull(setMethod, "No setter found for field: " + name);

        visitor.visitVarInsn(ALOAD, 2);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(ALOAD, 1);

        visitor.visitLdcInsn(name);

        visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(AttributeValue.class));

        visitor.visitLdcInsn(Type.getType(type));

        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(parserClass), parser,
                Type.getMethodDescriptor(readerMethod), false);

        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(setMethod.getDeclaringClass()),
                setMethod.getName(), Type.getMethodDescriptor(setMethod), false);

        if (setMethod.getReturnType() != void.class) {
            visitor.visitInsn(POP);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void handleComplex(@NotNull MethodVisitor visitor, String name, @Nullable Method setMethod,
                                      Class<?> type, @NotNull Class<?> parent, Class<? extends ObjectParser> parserClass) {

        @Nullable var readerMethod = getMethodByName("parseComplex", parserClass);

        Objects.requireNonNull(readerMethod, "No method found with name: parseComplex");

        if (parent.isRecord()) {

            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitVarInsn(ALOAD, 1);

            visitor.visitLdcInsn(name);

            visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(AttributeValue.class));

            visitor.visitLdcInsn(Type.getType(type));

            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(parserClass), "parseComplex",
                    Type.getMethodDescriptor(readerMethod), false);

            visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(type));

            return;
        }

        Objects.requireNonNull(setMethod);

        visitor.visitVarInsn(ALOAD, 2);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(ALOAD, 1);

        visitor.visitLdcInsn(name);

        visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(AttributeValue.class));

        visitor.visitLdcInsn(Type.getType(type));

        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(parserClass), "parseComplex",
                Type.getMethodDescriptor(readerMethod), false);

        visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(type));
        visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(setMethod.getDeclaringClass()),
                setMethod.getName(), Type.getMethodDescriptor(setMethod), false);

        //Builder-Pattern setters requires a pop after the method invocation
        if (setMethod.getReturnType() != void.class) {
            visitor.visitInsn(POP);
        }

    }

    @SuppressWarnings("rawtypes")
    private static void defineReadMethod(@NotNull ClassWriter writer, @NotNull Class<?> type,
                                         Class<? extends ObjectParser> parserClass) {

        if (type.isRecord()) {
            defineRecordParser(writer, type, parserClass);
            return;
        }

        // Define the parse method
        MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, "read", "(Ljava/util/Map;)Ljava/lang/Object;",
                "(Ljava/util/Map<Ljava/lang/String;LAttributeValue;>;)L" + Type.getInternalName(type) + ";", null);

        visitor.visitCode();
        visitor.visitTypeInsn(NEW, Type.getInternalName(type));
        visitor.visitInsn(DUP);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        visitor.visitVarInsn(ASTORE, 2);

        var stacks = generateMethods(visitor, type, parserClass);

        visitor.visitMaxs(stacks + 1, 3);

        visitor.visitVarInsn(ALOAD, 2);
        visitor.visitInsn(Opcodes.ARETURN);
        visitor.visitEnd();

    }

    @SuppressWarnings("rawtypes")
    private static void defineRecordParser(@NotNull ClassWriter writer, Class<?> type, Class<? extends ObjectParser> parserClass) {
        // Define the parse method
        MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, "read", "(Ljava/util/Map;)Ljava/lang/Object;",
                "(Ljava/util/Map<Ljava/lang/String;LAttributeValue;>;)L" + Type.getInternalName(type) + ";", null);

        visitor.visitCode();
        visitor.visitTypeInsn(NEW, Type.getInternalName(type));
        visitor.visitInsn(DUP);

        var stacks = generateMethods(visitor, type, parserClass);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('(');

        for (RecordComponent recordComponent : type.getRecordComponents()) {
            stringBuilder.append(Type.getType(recordComponent.getType()));
        }

        stringBuilder.append(")V");

        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>", stringBuilder.toString(), false);
        visitor.visitInsn(Opcodes.ARETURN);

        visitor.visitMaxs(stacks + 1, 3);
        visitor.visitEnd();
    }

    @SuppressWarnings("rawtypes")
    private static void defineConstructor(@NotNull ClassWriter writer, Class<? extends ObjectParser> parserClass) {
        MethodVisitor constructorMv = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructorMv.visitVarInsn(Opcodes.ALOAD, 0);
        constructorMv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(parserClass), "<init>", "()V", false); // Call the superclass constructor
        constructorMv.visitInsn(Opcodes.RETURN);
        constructorMv.visitMaxs(1, 1);
        constructorMv.visitEnd();
    }

    private static List<Field> getFields(@NotNull Class<?> type) {

        if (type.isRecord()) {
            return Arrays.stream(type.getDeclaredFields())
                    .toList();
        }

        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isAnnotationPresent(Transient.class) ||
                        !Modifier.isTransient(field.getModifiers()) ||
                        !Modifier.isFinal(field.getModifiers()) ||
                        !field.isAnnotationPresent(DynamoIgnore.class))
                .toList();
    }

    @SuppressWarnings("rawtypes")
    private static int generateMethods(MethodVisitor visitor, @NotNull Class<?> type, Class<? extends ObjectParser> parserClass) {

        var methodMap = Arrays.stream(type.getDeclaredMethods())
                .filter(a -> a.getParameterCount() == 1)
                .collect(Collectors.toMap(method -> method.getName().toLowerCase(), ignored -> ignored));

        if (methodMap.isEmpty()) throw new IllegalStateException("No modifiers found for class " + type);

        int stacks = 0;


        if (type.isRecord()) {

            for (RecordComponent component : type.getRecordComponents()) {
                computeReader(visitor, component.getName(), component.getType(), null,
                        type, getGenericType(component.getGenericType()), parserClass);


                stacks += 2;
            }

        } else {

            for (Field field : getFields(type)) {

                var setMethod = methodMap.get("set" + field.getName().toLowerCase());

                if (setMethod == null) {
                    continue;
                }

                //Should manage autoboxing...
                if (setMethod.getParameterTypes()[0] != field.getType()) {
                    throw new IllegalStateException("Incorrect mutator parameter type: '%s' expected '%s' from field"
                            .formatted(setMethod.getParameterTypes()[0], field.getType()));
                }

                stacks += 2;

                computeReader(visitor, field.getName(), field.getType(), setMethod, type, getGenericType(field), parserClass);
            }
        }

        return stacks;
    }


    static boolean isCustomClass(@NotNull Class<?> type) {
        return !type.getName().startsWith("java");
    }

    static boolean isJVMClass(@NotNull Class<?> type) {
        return type.getName().startsWith("java");
    }


    private static @Nullable Method getReaderMethod(Class<?> type, Class<?> parserClass) {

        if (type == List.class) return null;
        if (type == Map.class) return null;
        if (type == Set.class) return null;

        var converter = INSTANCE.converterMap.get(parserClass);

        if (converter == null) {
            converter = new ConverterStruct(parserClass);
            INSTANCE.converterMap.put(parserClass, converter);
        }

        return converter.getReader(type);
    }

    public static @Nullable Method getWriterMethod(Class<?> type, Class<?> parserClass) {

        var converter = INSTANCE.converterMap.get(parserClass);

        if (converter == null) {
            converter = new ConverterStruct(parserClass);
            INSTANCE.converterMap.put(parserClass, converter);
        }

        return converter.getWriter(type);
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull ObjectParser<T> createObject(byte[] bytecode, @NotNull Class<T> type, @NotNull Class<?> parserClass) {
        var clazz = INSTANCE.defineNewClass(bytecode, type.getSimpleName() + parserClass.getSimpleName());

        try {
            var constructor = clazz.getConstructor();

            return (ObjectParser<T>) constructor.newInstance();
        } catch (VerifyError | Exception ex) {
            throw new RuntimeException("Failed to compile converter for class '%s'".formatted(type), ex);
        }
    }

}
