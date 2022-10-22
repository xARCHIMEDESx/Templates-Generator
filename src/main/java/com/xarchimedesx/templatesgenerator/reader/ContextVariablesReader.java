package com.xarchimedesx.templatesgenerator.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.shaded.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextVariablesReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContextVariablesReader.class);
  private static final ClassLoader CLASS_LOADER = ContextVariablesReader.class.getClassLoader();
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
    return getFileStream(variablesPath).entrySet()
        .stream()
        .filter(e -> extensionFilter(e.getKey()))
        .map(e -> Pair.of(
            FilenameUtils.getBaseName(e.getKey()),
            getFileContent(e.getValue())
        ))
        .collect(Collectors.toList());
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

  private Map<String, BufferedInputStream> getFileStream(String path) {
    Map<String, BufferedInputStream> namedStreams = new HashMap<>();

    File localFile = new File(path);
    if (localFile.exists()) {
      try {
        System.out.println(localFile.toURI().toURL());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      return getLocalFileStream(localFile, namedStreams);
    } else {
      URL url = CLASS_LOADER.getResource(FilenameUtils.separatorsToUnix(path));
      System.out.println(url);
      if (url != null) {
        return getResourceFileStream(url);
      } else {
        throw new IllegalArgumentException("Variables file/directory \"" + path + "\" does not exist.");
      }
    }
  }

  private Map<String, BufferedInputStream> getLocalFileStream(File file, Map<String, BufferedInputStream> namedStreams) {
    try {
      BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
      if (basicFileAttributes.isRegularFile()) {
        namedStreams.put(file.getName(), new BufferedInputStream(Files.newInputStream(file.toPath())));
      }
      if (basicFileAttributes.isDirectory()) {
        File[] files = file.listFiles();
        if (files != null) {
          Arrays.stream(files).filter(File::isFile).forEach(f -> getLocalFileStream(f, namedStreams));
        } else {
          throw new IllegalArgumentException("Variables directory " + file.toPath() + " is empty.");
        }
      }
      return namedStreams;
    } catch (IOException ioe) {
      throw new IllegalArgumentException("Cannot open variables file: " + file.toPath());
    }
  }

  private Map<String, BufferedInputStream> getResourceFileStream(URL url) {
    if (url.getProtocol().equals("jar")) {
      return inspectJar(url);
    } else {
      return inspectResource(url);
    }
  }

  private Map<String, BufferedInputStream> inspectResource(URL url) {
    try (Stream<Path> stream = Files.walk(Paths.get(url.toURI()), 1)) {
      return stream.filter(Files::isRegularFile)
          .map(path -> {
            try {
              return Pair.of(path.toString(), new BufferedInputStream(Files.newInputStream(path)));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    } catch (IOException | URISyntaxException e) {
      throw new IllegalArgumentException("Cannot open variables file: " + url);
    }
  }

  private Map<String, BufferedInputStream> inspectJar(URL url) {
    try (FileSystem fs = FileSystems.newFileSystem(url.toURI(), Collections.emptyMap());
         Stream<Path> stream = Files.walk(Paths.get(url.toURI()), 1)) {
      return stream.filter(Files::isRegularFile)
          .map(path -> StringUtils.stripStart(path.toString(), "/"))
          .map(this::getJarFileStream)
          .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    } catch (IOException | URISyntaxException e) {
      throw new IllegalArgumentException("Cannot open variables file: " + url);
    }
  }

  private Pair<String, BufferedInputStream> getJarFileStream(String path) {
    InputStream input = CLASS_LOADER.getResourceAsStream(path);
    if (input != null) {
      return Pair.of(path, new BufferedInputStream(input));
    } else {
      throw new IllegalArgumentException("Cannot open variables file: " + path);
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
