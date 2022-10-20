package com.xarchimedesx.templatesgenerator.cli;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

  private static final String TEMPLATE = "TEMPLATE";
  private static final String VARIABLES = "VARIABLES";
  private static final String OUTPUT = "OUTPUT";
  private static final Parser PARSER = new Parser();

  @Test
  public void parserParseValidInput() {
    CommandLine cli = PARSER.parse("-t", TEMPLATE, "-v", VARIABLES, "-o", OUTPUT);
    assertEquals(TEMPLATE, cli.getOptionValue("template"));
    assertEquals(VARIABLES, cli.getOptionValue("variables"));
    assertEquals(OUTPUT, cli.getOptionValue("output"));
    assertFalse(cli.hasOption("combine"));
  }

  @Test
  public void parserParseValidInputWithCombine() {
    CommandLine cli = PARSER.parse("-t", TEMPLATE, "-v", VARIABLES, "-o", OUTPUT, "-c");
    assertEquals(TEMPLATE, cli.getOptionValue("template"));
    assertEquals(VARIABLES, cli.getOptionValue("variables"));
    assertEquals(OUTPUT, cli.getOptionValue("output"));
    assertTrue(cli.hasOption("combine"));
  }

  @Test
  public void parserCatchAnException() {
    assertThrows(RuntimeException.class, () -> PARSER.parse("Unknown parameters"));
  }
}
