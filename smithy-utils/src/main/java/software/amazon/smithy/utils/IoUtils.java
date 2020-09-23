/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilities for IO operations.
 */
public final class IoUtils {
    private static final int BUFFER_SIZE = 1024 * 4;

    private IoUtils() {}

    /**
     * Reads and returns the rest of the given input stream as a byte array.
     * Caller is responsible for closing the given input stream.
     *
     * @param is The input stream to convert.
     * @return The converted bytes.
     */
    public static byte[] toByteArray(InputStream is) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] b = new byte[BUFFER_SIZE];
            int n;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads and returns the rest of the given input stream as a string.
     * Caller is responsible for closing the given input stream.
     *
     * @param is The input stream to convert.
     * @return The converted string.
     */
    public static String toUtf8String(InputStream is) {
        return new String(toByteArray(is), StandardCharsets.UTF_8);
    }

    /**
     * Reads a file into a UTF-8 encoded string.
     *
     * @param path Path to the file to read.
     * @return Returns the contents of the file.
     * @throws RuntimeException if the file can't be read or encoded.
     */
    public static String readUtf8File(String path) {
        return readUtf8File(Paths.get(path));
    }

    /**
     * Reads a file into a UTF-8 encoded string.
     *
     * @param path Path to the file to read.
     * @return Returns the contents of the file.
     * @throws RuntimeException if the file can't be read or encoded.
     */
    public static String readUtf8File(Path path) {
        try {
            return new String(Files.readAllBytes(path.toRealPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads a class loader resource into a UTF-8 string.
     *
     * <p>This is equivalent to reading the contents of an {@link InputStream}
     * from {@link ClassLoader#getResourceAsStream}.
     *
     * @param classLoader Class loader to load from.
     * @param resourcePath Path to the resource to load.
     * @return Returns the loaded resource.
     * @throws UncheckedIOException if the resource cannot be loaded.
     */
    public static String readUtf8Resource(ClassLoader classLoader, String resourcePath) {
        return readUtf8Url(classLoader.getResource(resourcePath));
    }

    /**
     * Reads a class resource into a UTF-8 string.
     *
     * <p>This is equivalent to reading the contents of an {@link InputStream}
     * from {@link Class#getResourceAsStream(String)}.
     *
     * @param clazz Class to load from.
     * @param resourcePath Path to the resource to load.
     * @return Returns the loaded resource.
     * @throws UncheckedIOException if the resource cannot be loaded.
     */
    public static String readUtf8Resource(Class<?> clazz, String resourcePath) {
        return readUtf8Url(clazz.getResource(resourcePath));
    }

    /**
     * Reads a URL resource into a UTF-8 string.
     *
     * @param url URL to load from.
     * @return Returns the loaded resource.
     * @throws UncheckedIOException if the resource cannot be loaded.
     */
    public static String readUtf8Url(URL url) {
        try (InputStream is = url.openStream()) {
            return toUtf8String(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
