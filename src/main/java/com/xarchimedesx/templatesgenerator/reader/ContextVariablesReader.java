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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextVariablesReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContextVariablesReader.class);
  static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("json", "yaml", "yml");

  /**
   *
   * @param parsedVariablesPaths - list of paths to variables to be processed. Every path can be a file path or a directory path.
   * @param isCombined - Whether to combine multiple variables files' content inside single collection.
   * @return List of tuples, where the left side is the name of the file which was processed and the right side - its content.
   * If {@code isCombined == true} - merges all the content inside single collection.
   */

  public List<Pair<String, Map<String, Object>>> getVariables(List<String> parsedVariablesPaths, boolean isCombined) {
    List<Pair<String, Map<String, Object>>> variables;
    if (isCombined) {
      variables = Stream.of(
          Pair.of("COMBINED",
              parsedVariablesPaths.stream()
                  .map(this::processPath)
                  .flatMap(List::stream)
                  .map(Pair::getValue)
                  .flatMap(m -> m.entrySet().stream())
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Arrays::asList))
          )
      ).collect(Collectors.toList());
    } else {
      variables = parsedVariablesPaths.stream()
          .map(this::processPath)
          .flatMap(List::stream)
          .collect(Collectors.toList());
    }

    if (variables.isEmpty()) {
      LOGGER.warn("No data was read from provides variables paths!");
    }

    return variables;
  }

  // package-private for tests
  List<Pair<String, Map<String, Object>>> processPath(String variablesPath) {
    try {
      List<Pair<String, Map<String, Object>>> namedVariables = new ArrayList<>();
      URI uri = getUriFromPath(variablesPath);
      List<Path> discoveredPaths = inspectUri(uri);

      for (Path discoveredPath : discoveredPaths) {
        namedVariables.add(Pair.of(
            FilenameUtils.getBaseName(discoveredPath.toString()),
            getFileContent(getInputStream(discoveredPath, uri.getScheme()))
        ));
      }
      return namedVariables;
    } catch (IOException | URISyntaxException ex) {
      LOGGER.error("Cannot open variables file: {}", variablesPath);
      throw new RuntimeException(ex);
    }
  }

  private URI getUriFromPath(String variablesPath) throws URISyntaxException {
    File localFile = new File(variablesPath);
    if (localFile.exists()) {
      return localFile.toURI();
    } else {
      URL resource = ContextVariablesReader.class.getClassLoader().getResource(FilenameUtils.separatorsToUnix(variablesPath));
      if (resource != null) {
        return resource.toURI();
      } else {
        throw new IllegalArgumentException("Variables file/directory \"" + variablesPath + "\" does not exist.");
      }
    }
  }

  private List<Path> inspectUri(URI uri) throws URISyntaxException, IOException {
    String scheme = uri.getScheme();
    if (scheme.equals("file")) {
      return walkThroughPath(uri);
    } else if (scheme.equals("jar")) {
      // to properly convert a URI to a Path we need to create a JAR file system first
      try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
        return walkThroughPath(uri);
      }
    } else {
      throw new IllegalArgumentException("Unsupported scheme in given URI: " + uri);
    }
  }

  private List<Path> walkThroughPath(URI uri) throws IOException {
    try (Stream<Path> stream = Files.walk(Paths.get(uri))) {
      return stream.filter(Files::isRegularFile)
          .filter(this::extensionFilter)
          .sorted()
          .collect(Collectors.toList());
    }
  }

  private BufferedInputStream getInputStream(Path path, String scheme) throws IOException {
    InputStream inputStream = scheme.equals("file")
        ? Files.newInputStream(path)
        : ContextVariablesReader.class.getResourceAsStream(path.toString());
    if (inputStream != null) {
      return new BufferedInputStream(inputStream);
    } else {
      throw new IOException("Cannot open input stream for file: " + path);
    }
  }

  private Map<String, Object> getFileContent(BufferedInputStream fileStream) throws IOException {
    return new ObjectMapper(new YAMLFactory()).readValue(fileStream, new TypeReference<Map<String, Object>>() {
    });
  }

  // package-private for tests
  boolean extensionFilter(Path path) {
    if (SUPPORTED_EXTENSIONS.contains(FilenameUtils.getExtension(path.toString()))) {
      return true;
    } else {
      LOGGER.warn("Unrecognized variables file format! Templates-generator supports only .json and .yml/.yaml file extensions.");
      LOGGER.warn("Skipping file: {}", path);
      return false;
    }
  }
}
