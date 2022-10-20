package com.xarchimedesx.templatesgenerator;

import com.xarchimedesx.templatesgenerator.cli.Parser;
import com.xarchimedesx.templatesgenerator.directive.SaveFileDirective;
import com.xarchimedesx.templatesgenerator.exception.NotInitializedException;
import com.xarchimedesx.templatesgenerator.reader.ContextVariablesReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.apache.velocity.shaded.commons.io.FilenameUtils;
import org.apache.velocity.tools.ToolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TemplatesGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(TemplatesGenerator.class);
  private static final String RESOURCE_LOADERS_NAMES = "file,classpath";
  private static final String FILE_RESOURCE_LOADER = "resource.loader.file.class";
  private static final String CLASSPATH_RESOURCE_LOADER = "resource.loader.classpath.class";
  private static final String TOOLS_CONFIG_FILE = "toolsConfiguration.xml";
  private static final String OUTPUT_DIR_BASE_PATH_REFERENCE_NAME = "outputDirBasePath";
  static final String VARIABLES_FILES_SEPARATOR = ",";
  private ContextVariablesReader reader;
  private Template template;
  private Context velocityContext;

  public static void main(String[] args) {
    CommandLine cli = new Parser().parse(args);

    String templatePath = cli.getOptionValue("template");
    String variablesPaths = cli.getOptionValue("variables");
    String outputDirBasePath = cli.getOptionValue("output");
    boolean isCombined = cli.hasOption("combine");

    TemplatesGenerator tg = new TemplatesGenerator();
    tg.initialize(templatePath);
    tg.render(variablesPaths, outputDirBasePath, isCombined);
  }

  public void initialize(String templatePath) {
    templatePath = FilenameUtils.normalize(templatePath);
    LOGGER.info("Initializing Templates-Generator with\n    Velocity template path: {}", templatePath);
    VelocityEngine engine = initializeAndGetVelocityEngine(FilenameUtils.getFullPath(templatePath));
    this.template = engine.getTemplate(templatePath);
    this.velocityContext = new VelocityContext(initializeAndGetToolContext(engine));
    this.reader = new ContextVariablesReader();
  }

  public void render(String variablesPaths, String outputDirBasePath, boolean isCombined) {
    if (velocityContext != null) {
      List<String> parsedVariablesPaths = preprocessVariablesPaths(variablesPaths);
      outputDirBasePath = FilenameUtils.normalize(outputDirBasePath);
      LOGGER.info("Running rendering with\n    Variables files paths: {}\n    Output path: {}\n    Is combined: {}",
          parsedVariablesPaths, outputDirBasePath, isCombined);

      List<Pair<String, Map<String, Object>>> variables = reader.processPaths(parsedVariablesPaths, isCombined);
      int filesToBeSaved = variables.size();

      for (Pair<String, Map<String, Object>> var : variables) {
        String outputPath = formOutputPath(filesToBeSaved, outputDirBasePath, var.getKey());
        updateVelocityContextWithVariables(var.getValue(), outputPath);
        mergeTemplateAndVelocityContext();
      }
    } else {
      throw new NotInitializedException("Templates-Generator has not been properly initialized. " +
          "Have you forgotten to call 'initialize()' method first?");
    }
  }

  private VelocityEngine initializeAndGetVelocityEngine(String templateDirPath) {
    VelocityEngine engine = new VelocityEngine();
    engine.setProperty(RuntimeConstants.CUSTOM_DIRECTIVES, SaveFileDirective.class.getName());
    engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, RESOURCE_LOADERS_NAMES);
    engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templateDirPath);
    engine.setProperty(FILE_RESOURCE_LOADER, FileResourceLoader.class.getName());
    engine.setProperty(CLASSPATH_RESOURCE_LOADER, ClasspathResourceLoader.class.getName());
    engine.init();

    return engine;
  }

  private Context initializeAndGetToolContext(VelocityEngine engine) {
    ToolManager toolManager = new ToolManager(false, true);
    toolManager.configure(TOOLS_CONFIG_FILE);
    toolManager.setVelocityEngine(engine);

    return toolManager.createContext();
  }

  private void updateVelocityContextWithVariables(Map<String, Object> variables, String outputDirBasePath) {
    variables.forEach(velocityContext::put);
    velocityContext.put(OUTPUT_DIR_BASE_PATH_REFERENCE_NAME, outputDirBasePath);
  }

  private void mergeTemplateAndVelocityContext() {
    try (Writer writer = new StringWriter()) {
      template.merge(velocityContext, writer);
    } catch (IOException ioe) {
      LOGGER.error("Exception occurred while merging template and context: {}", ioe.getMessage());
      throw new RuntimeException(ioe);
    }
  }

  private List<String> preprocessVariablesPaths(String variablesPaths) {
    return Arrays.stream(variablesPaths.split(VARIABLES_FILES_SEPARATOR))
        .map(String::trim)
        .map(FilenameUtils::normalize)
        .collect(Collectors.toList());
  }

  private String formOutputPath(int filesToBeSaved, String outputDirBasePath, String renderedFileSubpath) {
    if (filesToBeSaved == 1)
      return outputDirBasePath;
    else
      return String.join("/", outputDirBasePath, renderedFileSubpath);
  }
}
