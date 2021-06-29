package com.xarchimedesx.templatesgenerator;

import com.xarchimedesx.templatesgenerator.reader.ContextVariablesReader;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContextVariablesReaderTest {

  private static final String VARIABLES_PATH = "variables.yaml";

  @Test
  void readVariables() {
    Map<String, Object> expectedVariables = Stream.of(
        new AbstractMap.SimpleEntry<>("users", Stream.of(
            Stream.of(
                new AbstractMap.SimpleEntry<>("id", 1),
                new AbstractMap.SimpleEntry<>("personal", Stream.of(
                    new AbstractMap.SimpleEntry<>("age", 26),
                    new AbstractMap.SimpleEntry<>("name", "John"),
                    new AbstractMap.SimpleEntry<>("surname", "Doe")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            Stream.of(
                new AbstractMap.SimpleEntry<>("id", 2),
                new AbstractMap.SimpleEntry<>("personal", Stream.of(
                    new AbstractMap.SimpleEntry<>("age", 28),
                    new AbstractMap.SimpleEntry<>("name", "Jane"),
                    new AbstractMap.SimpleEntry<>("surname", "Doe")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).collect(Collectors.toList())
        )
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Assertions.assertEquals(expectedVariables, new ContextVariablesReader().readVariables(VARIABLES_PATH));
  }
}
