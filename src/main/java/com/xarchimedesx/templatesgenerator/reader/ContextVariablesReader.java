package com.xarchimedesx.templatesgenerator.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.shaded.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextVariablesReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContextVariablesReader.class);
  static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("json", "yaml", "yml");

  public List<Pair<String, Map<String, Object>>> processPaths(List<String> parsedVariablesPaths, boolean isCombined) {
    List<Pair<String, Map<String, Object>>> variables;
    if (isCombined) {
      variables = Stream.of(
          Pair.of("COMBINED",
              parsedVariablesPaths.stream()
                  .map(this::getNamedVariables)
                  .flatMap(List::stream)
                  .map(Pair::getValue)
                  .flatMap(m -> m.entrySet().stream())
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Arrays::asList))
          )
      ).collect(Collectors.toList());
    } else {
      variables = parsedVariablesPaths.stream()
          .map(this::getNamedVariables)
          .flatMap(List::stream)
          .collect(Collectors.toList());
    }

    if (variables.isEmpty()) {
      LOGGER.warn("No data was read from provides variables paths!");
    }

    return variables;
  }

  // package-private for tests
  List<Pair<String, Map<String, Object>>> getNamedVariables(String variablesPath) {
    return inspectPath(variablesPath).entrySet()
        .stream()
        .filter(e -> extensionFilter(e.getKey()))
        .map(e -> Pair.of(
            FilenameUtils.getBaseName(e.getKey()),
            getFileContent(e.getValue())
        ))
        .collect(Collectors.toList());
  }

  private Map<String, BufferedInputStream> inspectPath(String path) {
    try {
      File localFile = new File(path);
      URL url = localFile.exists()
          ? localFile.toURI().toURL()
          : ContextVariablesReader.class.getClassLoader().getResource(FilenameUtils.separatorsToUnix(path));

      if (url != null) {
        String protocol = url.getProtocol();
        URI uri = url.toURI();
        if (protocol.equals("file")) {
          return walkThroughPath(uri, protocol);
        } else if (protocol.equals("jar")) {
          // to convert a URI to a Path we need to create a JAR file system first
          try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            return walkThroughPath(uri, protocol);
          }
        } else {
          throw new IllegalArgumentException("Unsupported protocol for given URL: " + url);
        }
      } else {
        throw new IllegalArgumentException("Variables file/directory \"" + path + "\" does not exist.");
      }
    } catch (IOException | URISyntaxException ex) {
      LOGGER.error("Cannot open variables file: {}", path);
      throw new IllegalArgumentException(ex);
    }
  }

  private Map<String, BufferedInputStream> walkThroughPath(URI uri, String protocol) throws URISyntaxException, IOException {
    try (Stream<Path> stream = Files.walk(Paths.get(uri), 1)) {
      return stream.filter(Files::isRegularFile)
          .map(path -> Pair.of(path.toString(), getInputStream(path, protocol)))
          .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }
  }

  private BufferedInputStream getInputStream(Path path, String protocol) {
    try {
      InputStream inputStream = protocol.equals("file")
          ? Files.newInputStream(path)
          : ContextVariablesReader.class.getResourceAsStream(String.valueOf(path));

      if (inputStream != null) {
        return new BufferedInputStream(inputStream);
      } else {
        throw new IllegalArgumentException("Cannot open variables file: " + path);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, Object> getFileContent(BufferedInputStream fileStream) {
    try {
      return new ObjectMapper(new YAMLFactory()).readValue(fileStream, new TypeReference<Map<String, Object>>() {
      });
    } catch (IOException ioe) {
      LOGGER.error("Exception occurred while reading variables from file: {}", ioe.getMessage());
      throw new RuntimeException(ioe);
    }
  }

  // package-private for tests
  boolean extensionFilter(String fileName) {
    if (SUPPORTED_EXTENSIONS.contains(FilenameUtils.getExtension(fileName))) {
      return true;
    } else {
      LOGGER.warn("Unrecognized variables file format! Templates-generator supports only .json and .yml/.yaml file extensions.");
      LOGGER.warn("Skipping file: {}", fileName);
      return false;
    }
  }
}
