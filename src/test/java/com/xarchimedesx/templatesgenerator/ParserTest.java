package com.xarchimedesx.templatesgenerator;

import com.xarchimedesx.templatesgenerator.cli.Parser;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParserTest {

  private static final String TEMPLATE = "TEMPLATE";
  private static final String VARIABLES = "VARIABLES";
  private static final String OUTPUT = "OUTPUT";
  private static final Parser PARSER = new Parser();

  @Test
  public void parserParseValidInput() {
    CommandLine cli = PARSER.parse("-t", TEMPLATE, "-v", VARIABLES, "-o", OUTPUT);
    Assertions.assertEquals(TEMPLATE, cli.getOptionValue("template"));
    Assertions.assertEquals(VARIABLES, cli.getOptionValue("variables"));
    Assertions.assertEquals(OUTPUT, cli.getOptionValue("output"));
  }

  @Test
  public void parserParseValidInputOutOfOrder() {
    CommandLine cli = PARSER.parse("-v", VARIABLES, "-o", OUTPUT, "-t", TEMPLATE);
    Assertions.assertEquals(TEMPLATE, cli.getOptionValue("template"));
    Assertions.assertEquals(VARIABLES, cli.getOptionValue("variables"));
    Assertions.assertEquals(OUTPUT, cli.getOptionValue("output"));
  }

  @Test
  public void parserCatchAnException() {
    Assertions.assertThrows(RuntimeException.class, () -> PARSER.parse("Unknown parameters"));
  }
}
