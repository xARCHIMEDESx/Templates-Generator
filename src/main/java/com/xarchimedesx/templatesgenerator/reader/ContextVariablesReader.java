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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    if(variables.isEmpty()){
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
      return getLocalFileStream(localFile, namedStreams);
    } else {
      String refinedPath = FilenameUtils.separatorsToUnix(path);
      URL resource = CLASS_LOADER.getResource(refinedPath);
      if (resource != null) {
        if (resource.getProtocol().equals("file")) {
          LOGGER.debug(String.valueOf(new File(resource.getPath()).isDirectory()));
        }
        return getLocalFileStream(new File(resource.getPath()), namedStreams);
        //return getFileStreamFromClassPath(refinedPath, namedSteams);
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

  private Map<String, BufferedInputStream> getFileStreamFromClassPath(String path, Map<String, BufferedInputStream> namedStreams) {
    InputStream input = CLASS_LOADER.getResourceAsStream(path);
    if (input != null) {
      namedStreams.put(path, new BufferedInputStream(input));
      return namedStreams;
    } else {
      throw new IllegalArgumentException("CHECK ME LATER");
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
