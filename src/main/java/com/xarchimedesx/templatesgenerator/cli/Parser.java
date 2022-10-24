package com.xarchimedesx.templatesgenerator.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parser {

  private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

  public CommandLine parse(String... args) {

    Options options = new Options();
    options.addRequiredOption("t", "template", true, "Velocity template path.");
    options.addRequiredOption("v", "variables", true, "Variables file/dir path.");
    options.addRequiredOption("o", "output", true, "Output path.");
    options.addOption("c", "combine", false, "Combine variables files in given dir inside single context");

    try {
      return new DefaultParser().parse(options, args);
    } catch (ParseException pe) {
      new HelpFormatter().printHelp("java -jar templates-generator.jar", options, true);
      LOGGER.error("An error occurred while parsing command-line arguments: {}", pe.getMessage());
      throw new RuntimeException(pe);
    }
  }
}
