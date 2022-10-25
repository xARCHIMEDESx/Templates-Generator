package com.xarchimedesx.templatesgenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplatesGeneratorTest {

  private static final String TEMPLATE_PATH = String.join(File.separator, "templates", "template.vm");
  private static final String COMBINED_TEMPLATE_PATH = String.join(File.separator, "templates", "combined_template.vm");
  private static final String INVALID_TEMPLATE_PATH = String.join(File.separator, "templates", "invalid_template.vm");
  private static final String NONEXISTING_TEMPLATE_PATH = "nonexisting_template.vm";

  private static final String USERS_DIR_PATH = String.join(File.separator, "variables", "users");
  private static final String USERS1_PATH = String.join(File.separator, USERS_DIR_PATH, "users1.yaml");
  private static final String GROUPS_PATH = String.join(File.separator, "variables", "groups.yml");

  private static final String EXPECTED_FILE1_PATH = String.join(File.separator, "expected", "expected1.json");
  private static final String EXPECTED_FILE2_PATH = String.join(File.separator, "expected", "expected2.json");
  private static final String EXPECTED_FILE_COMBINED_PATH = String.join(File.separator, "expected", "expected_with_groups.json");

  private static final String OUTPUT_DIR_BASE_PATH = String.join(File.separator, "target", "test-data");
  private static final String RENDERED_USERS_PATH = String.join(File.separator, OUTPUT_DIR_BASE_PATH, "users.json");
  private static final String RENDERED_USERS_WITH_GROUPS_PATH = String.join(File.separator, OUTPUT_DIR_BASE_PATH, "users_with_groups.json");
  private static final String RENDERED_USERS1_PATH = String.join(File.separator, OUTPUT_DIR_BASE_PATH, "users1", "users.json");
  private static final String RENDERED_USERS2_PATH = String.join(File.separator, OUTPUT_DIR_BASE_PATH, "users2", "users.json");

  private static final TemplatesGenerator GENERATOR = new TemplatesGenerator();
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ClassLoader CLASS_LOADER = TemplatesGeneratorTest.class.getClassLoader();

  @Test
  public void renderOneFile() throws IOException {
    GENERATOR.render(TEMPLATE_PATH, USERS1_PATH, OUTPUT_DIR_BASE_PATH, false);
    JsonNode expectedContent = MAPPER.readTree(CLASS_LOADER.getResourceAsStream(EXPECTED_FILE1_PATH));
    JsonNode renderedContent = MAPPER.readTree(Paths.get(RENDERED_USERS_PATH).toFile());

    assertEquals(expectedContent, renderedContent);
  }

  @Test
  public void renderFilesInDirectoryNotCombined() throws IOException {
    GENERATOR.render(TEMPLATE_PATH, USERS_DIR_PATH, OUTPUT_DIR_BASE_PATH, false);
    JsonNode expectedContent1 = MAPPER.readTree(CLASS_LOADER.getResourceAsStream(EXPECTED_FILE1_PATH));
    JsonNode renderedContent1 = MAPPER.readTree(Paths.get(RENDERED_USERS1_PATH).toFile());
    JsonNode expectedContent2 = MAPPER.readTree(CLASS_LOADER.getResourceAsStream(EXPECTED_FILE2_PATH));
    JsonNode renderedContent2 = MAPPER.readTree(Paths.get(RENDERED_USERS2_PATH).toFile());

    assertEquals(expectedContent1, renderedContent1);
    assertEquals(expectedContent2, renderedContent2);
  }

  @Test
  public void renderDirectoryAndAdditionalFileCombined() throws IOException {
    GENERATOR.render(COMBINED_TEMPLATE_PATH, String.join(TemplatesGenerator.VARIABLES_FILES_SEPARATOR, USERS_DIR_PATH, GROUPS_PATH),
        OUTPUT_DIR_BASE_PATH, true);

    JsonNode expectedContent = MAPPER.readTree(CLASS_LOADER.getResourceAsStream(EXPECTED_FILE_COMBINED_PATH));
    JsonNode renderedContent = MAPPER.readTree(Paths.get(RENDERED_USERS_WITH_GROUPS_PATH).toFile());

    assertEquals(expectedContent, renderedContent);
  }

  @Test
  public void failOnInvalidTemplate() {
    assertThrows(ParseErrorException.class, () -> GENERATOR.render(INVALID_TEMPLATE_PATH, USERS1_PATH, OUTPUT_DIR_BASE_PATH, false));
  }

  @Test
  public void failOnNonExistingTemplate() {
    assertThrows(ResourceNotFoundException.class, () -> GENERATOR.render(NONEXISTING_TEMPLATE_PATH, USERS1_PATH, OUTPUT_DIR_BASE_PATH, false));
  }
}
