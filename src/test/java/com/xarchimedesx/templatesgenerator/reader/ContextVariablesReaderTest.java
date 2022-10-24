package com.xarchimedesx.templatesgenerator.reader;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ContextVariablesReaderTest {

  private static final String USERS_DIR_PATH = String.join(File.separator, "variables", "users");
  private static final String USERS1_PATH = String.join(File.separator, USERS_DIR_PATH, "users1.yaml");
  private static final String USERS2_PATH = String.join(File.separator, USERS_DIR_PATH, "users2.json");
  private static final String GROUPS_PATH = String.join(File.separator, "variables", "groups.yml");
  private static final String NONEXISTING_VARIABLES_PATH = "nonexisting.yaml";
  private static final String INVALID_VARIABLES_PATH = String.join(File.separator, "variables", "invalid_variables.yaml");
  private static final ContextVariablesReader READER = new ContextVariablesReader();

  private static List<Pair<String, Map<String, Object>>> expectedUsers1Content;
  private static List<Pair<String, Map<String, Object>>> expectedUsers2Content;
  private static List<Pair<String, Map<String, Object>>> expectedGroupsContent;
  private static List<Pair<String, Map<String, Object>>> expectedAllContentCombined;
  private static List<Pair<String, Map<String, Object>>> expectedDirContent;
  private static List<Pair<String, Map<String, Object>>> expectedAllContentNonCombined;

  @BeforeAll
  static void setExpectedVariables() {
    expectedUsers1Content = Stream.of(
        Pair.of("users1", Stream.of(
            new SimpleEntry<String, Object>("users", Stream.of(
                Stream.of(
                    new SimpleEntry<>("id", 1),
                    new SimpleEntry<>("group_id", 2),
                    new SimpleEntry<>("personal", Stream.of(
                        new SimpleEntry<>("age", 26),
                        new SimpleEntry<>("name", "John"),
                        new SimpleEntry<>("surname", "Doe")
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                Stream.of(
                    new SimpleEntry<>("id", 2),
                    new SimpleEntry<>("group_id", 1),
                    new SimpleEntry<>("personal", Stream.of(
                        new SimpleEntry<>("age", 28),
                        new SimpleEntry<>("name", "Jane"),
                        new SimpleEntry<>("surname", "Doe")
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            ).collect(Collectors.toList()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
    ).collect(Collectors.toList());

    expectedUsers2Content = Stream.of(
        Pair.of("users2", Stream.of(
            new SimpleEntry<String, Object>("users", Stream.of(
                Stream.of(
                    new SimpleEntry<>("id", 3),
                    new SimpleEntry<>("group_id", 777),
                    new SimpleEntry<>("personal", Stream.of(
                        new SimpleEntry<>("age", 999),
                        new SimpleEntry<>("name", "Mary"),
                        new SimpleEntry<>("surname", "Poppins")
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                Stream.of(
                    new SimpleEntry<>("id", 4),
                    new SimpleEntry<>("group_id", 2),
                    new SimpleEntry<>("personal", Stream.of(
                        new SimpleEntry<>("age", 12),
                        new SimpleEntry<>("name", "Peter"),
                        new SimpleEntry<>("surname", "Pan")
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            ).collect(Collectors.toList()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
    ).collect(Collectors.toList());

    expectedGroupsContent = Stream.of(
        Pair.of("groups", Stream.of(
            new SimpleEntry<String, Object>("groups", Stream.of(
                Stream.of(
                    new SimpleEntry<>("id", 1),
                    new SimpleEntry<>("name", "employees")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                Stream.of(
                    new SimpleEntry<>("id", 2),
                    new SimpleEntry<>("name", "customers")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            ).collect(Collectors.toList()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
    ).collect(Collectors.toList());

    expectedAllContentCombined = Stream.of(
        Pair.of("COMBINED", Stream.of(
            new SimpleEntry<String, Object>("users", Stream.of(
                Stream.of(
                    Stream.of(
                        new SimpleEntry<>("id", 1),
                        new SimpleEntry<>("group_id", 2),
                        new SimpleEntry<>("personal", Stream.of(
                            new SimpleEntry<>("age", 26),
                            new SimpleEntry<>("name", "John"),
                            new SimpleEntry<>("surname", "Doe")
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    Stream.of(
                        new SimpleEntry<>("id", 2),
                        new SimpleEntry<>("group_id", 1),
                        new SimpleEntry<>("personal", Stream.of(
                            new SimpleEntry<>("age", 28),
                            new SimpleEntry<>("name", "Jane"),
                            new SimpleEntry<>("surname", "Doe")
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).collect(Collectors.toList()),
                Stream.of(
                    Stream.of(
                        new SimpleEntry<>("id", 3),
                        new SimpleEntry<>("group_id", 777),
                        new SimpleEntry<>("personal", Stream.of(
                            new SimpleEntry<>("age", 999),
                            new SimpleEntry<>("name", "Mary"),
                            new SimpleEntry<>("surname", "Poppins")
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    Stream.of(
                        new SimpleEntry<>("id", 4),
                        new SimpleEntry<>("group_id", 2),
                        new SimpleEntry<>("personal", Stream.of(
                            new SimpleEntry<>("age", 12),
                            new SimpleEntry<>("name", "Peter"),
                            new SimpleEntry<>("surname", "Pan")
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).collect(Collectors.toList())
            ).collect(Collectors.toList())),
            new SimpleEntry<String, Object>("groups", Stream.of(
                Stream.of(
                    new SimpleEntry<>("id", 1),
                    new SimpleEntry<>("name", "employees")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                Stream.of(
                    new SimpleEntry<>("id", 2),
                    new SimpleEntry<>("name", "customers")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            ).collect(Collectors.toList()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
    ).collect(Collectors.toList());

    expectedDirContent = Stream.concat(expectedUsers1Content.stream(), expectedUsers2Content.stream())
        .collect(Collectors.toList());

    expectedAllContentNonCombined = Stream.concat(expectedDirContent.stream(), expectedGroupsContent.stream())
        .collect(Collectors.toList());
  }

  @Test
  public void readYaml() {
    assertEquals(expectedUsers1Content, READER.processPath(USERS1_PATH));
  }

  @Test
  public void readJson() {
    assertEquals(expectedUsers2Content, READER.processPath(USERS2_PATH));
  }

  @Test
  public void readDirectory() {
    assertEquals(expectedDirContent, READER.processPath(USERS_DIR_PATH));
  }

  @Test
  public void failOnInvalidVariables() {
    assertThrows(RuntimeException.class, () -> READER.processPath(INVALID_VARIABLES_PATH));
  }

  @Test
  public void failOnNonExistingVariables() {
    assertThrows(IllegalArgumentException.class, () -> READER.processPath(NONEXISTING_VARIABLES_PATH));
  }

  @Test
  public void multifileInputNonCombined() {
    assertEquals(expectedAllContentNonCombined,
        READER.getVariables(Arrays.asList(USERS_DIR_PATH, GROUPS_PATH), false));
  }

  @Test
  public void multifileInputCombined() {
    assertEquals(expectedAllContentCombined,
        READER.getVariables(Arrays.asList(USERS_DIR_PATH, GROUPS_PATH), true));
  }

  @Test
  public void extensionFilterSupportedTest() {
    ContextVariablesReader.SUPPORTED_EXTENSIONS.stream()
        .map(e -> "file." + e)
        .forEach(f -> assertTrue(READER.extensionFilter(Paths.get(f))));
  }

  @Test
  public void extensionFilterNonSupportedTest() {
    assertFalse(READER.extensionFilter(Paths.get("file.txt")));
  }
}
