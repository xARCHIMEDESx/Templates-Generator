package com.xarchimedesx.templatesgenerator.reader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.velocity.shaded.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextVariablesReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContextVariablesReader.class);

  public Map<String, Object> readVariables(String variablesPath) {
    String fileExtension = FilenameUtils.getExtension(variablesPath);
    if (fileExtension.equalsIgnoreCase("json")
        || fileExtension.equalsIgnoreCase("yml")
        || fileExtension.equalsIgnoreCase("yaml")) {
      try {
        return new ObjectMapper(new YAMLFactory()).readValue(getFileStream(variablesPath), new TypeReference<Map<String, Object>>() {
        });
      } catch (IOException ioe) {
        LOGGER.error("Exception occurred while reading variables from file: {}", ioe.getMessage());
        throw new RuntimeException(ioe);
      }
    } else {
      throw new IllegalArgumentException("Unrecognized variables file format! Templates-generator supports only .json and .yml/.yaml file extensions");
    }
  }

  private BufferedInputStream getFileStream(String path) {
    try {
      File localFile = new File(path);
      if (localFile.exists() && localFile.isFile()) {
        return new BufferedInputStream(new FileInputStream(localFile));
      } else {
        return getFileStreamFromJar(path);
      }
    } catch (IOException ioe) {
      throw new IllegalArgumentException("Cannot open variables file: " + path);
    }
  }

  private BufferedInputStream getFileStreamFromJar(String path) {
    InputStream input = ContextVariablesReader.class.getClassLoader().getResourceAsStream(FilenameUtils.separatorsToUnix(path));
    if (input != null) {
      return new BufferedInputStream(input);
    } else {
      throw new IllegalArgumentException("Variables file " + path + " does not exist.");
    }
  }
}
