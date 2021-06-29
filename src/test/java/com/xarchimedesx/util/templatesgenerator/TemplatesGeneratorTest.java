package com.xarchimedesx.util.templatesgenerator;

import com.xarchimedesx.util.templatesgenerator.reader.ContextVariablesReader;

import java.io.File;

import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TemplatesGeneratorTest {

  private static final String TEMPLATE = "template.vm";
  private static final String INVALID_TEMPLATE = "invalid_template.vm";
  private static final String TEMPLATE_PATH_NOT_EXISTS = "nonexisting.vm";
  private static final String VARIABLES = "variables.yaml";
  private static final String INVALID_VARIABLES = "invalid_variables.yaml";
  private static final String VARIABLES_NOT_EXISTS = "nonexisting.yaml";
  private static final String VARIABLES_NOT_SUPPORTED = "nonsupported.txt";
  private static final String OUTPUT_DIR_BASE_PATH = "target";
  private static final String RENDERED_FILE = "test-data" + File.separator + "rendered.json";
  private static final String EXPECTED_FILE = "expected.json";
  private static final ContextVariablesReader READER = new ContextVariablesReader();


  @Test
  public void templatesGeneratorPositiveCase() {
    TemplatesGenerator.main(new String[]{"-t", TEMPLATE, "-v", VARIABLES, "-o", OUTPUT_DIR_BASE_PATH});
    Assertions.assertEquals(
        READER.readVariables(EXPECTED_FILE),
        READER.readVariables(OUTPUT_DIR_BASE_PATH + File.separator + RENDERED_FILE)
    );
  }

  @Test
  public void templatesGeneratorNegativeCaseInvalidTemplate() {
    Assertions.assertThrows(ParseErrorException.class, () -> TemplatesGenerator.main(
        new String[]{"-t", INVALID_TEMPLATE, "-v", VARIABLES, "-o", OUTPUT_DIR_BASE_PATH})
    );
  }

  @Test
  public void templatesGeneratorNegativeCaseNonexistingTemplate() {
    Assertions.assertThrows(ResourceNotFoundException.class, () -> TemplatesGenerator.main(
        new String[]{"-t", TEMPLATE_PATH_NOT_EXISTS, "-v", VARIABLES, "-o", OUTPUT_DIR_BASE_PATH})
    );
  }

  @Test
  public void templatesGeneratorNegativeCaseInvalidVariables() {
    Assertions.assertThrows(RuntimeException.class, () -> TemplatesGenerator.main(
        new String[]{"-t", TEMPLATE, "-v", INVALID_VARIABLES, "-o", OUTPUT_DIR_BASE_PATH})
    );
  }

  @Test
  public void templatesGeneratorNegativeCaseNonexistingVariables() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> TemplatesGenerator.main(
        new String[]{"-t", TEMPLATE, "-v", VARIABLES_NOT_EXISTS, "-o", OUTPUT_DIR_BASE_PATH})
    );
  }

  @Test
  public void templatesGeneratorNegativeCaseInvalidVariablesFormat() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> TemplatesGenerator.main(
        new String[]{"-t", TEMPLATE, "-v", VARIABLES_NOT_SUPPORTED, "-o", OUTPUT_DIR_BASE_PATH})
    );
  }
}
