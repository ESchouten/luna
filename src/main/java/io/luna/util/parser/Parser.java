package io.luna.util.parser;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstraction model providing functionality for parsing of various types of files.
 *
 * @param <T1> The reader that will be parsing the file.
 * @param <T2> The type of Object to create from parsing.
 * @author lare96 <http://github.org/lare96>
 */
public abstract class Parser<T1, T2> implements Runnable {

    /**
     * The asynchronous logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public final void run() {
        for (String pathString : forFiles()) {
            Path path = Paths.get(pathString);

            try (BufferedReader in = Files.newBufferedReader(path)) {
                List<T2> readObjects = new ArrayList<>();
                T1 reader = getReader(in);

                while (canRead(reader)) {
                    readObjects.add(doRead(reader));
                }
                onReadComplete(readObjects);
            } catch (Exception e) {
                LOGGER.error(new ParameterizedMessage("Error while reading file [{}]", path, e));
            }
        }
    }

    /**
     * Read the contents of the file with {@code reader}.
     *
     * @param reader The reader to parse the file with.
     * @return The {@code Object} that was read from the {@code reader}.
     * @throws Exception If any errors occur while reading.
     */
    public abstract T2 doRead(T1 reader) throws Exception;

    /**
     * Gets the reader of the file through {@code in}.
     *
     * @param in The {@link BufferedReader} used to get the reader of the file.
     * @return The reader of the file.
     * @throws Exception If any errors occur while getting the reader.
     */
    public abstract T1 getReader(BufferedReader in) throws Exception;

    /**
     * Determines if the {@code objectReader} can read another {@code Object}.
     *
     * @param objectReader The reader for the {@code Object}s being parsed.
     * @return {@code true} if more data can be read, {@code false} otherwise.
     * @throws Exception If any errors occur while determining the read status.
     */
    public abstract boolean canRead(T1 objectReader) throws Exception;

    /**
     * Invoked when this parser finishes parsing all {@code Object}s.
     *
     * @param readObjects The list of {@code Object}s that were parsed, possibly with a size of {@code 0}.
     * @throws Exception If any errors occur while executing completion logic.
     */
    public abstract void onReadComplete(List<T2> readObjects) throws Exception;

    /**
     * Retrieves an immutable list of files to parse.
     *
     * @return The files to parse.
     */
    public abstract ImmutableList<String> forFiles();
}
