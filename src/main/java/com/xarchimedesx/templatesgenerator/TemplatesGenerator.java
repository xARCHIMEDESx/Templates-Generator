package com.xarchimedesx.templatesgenerator;

import com.xarchimedesx.templatesgenerator.cli.Parser;
import com.xarchimedesx.templatesgenerator.directive.SaveFileDirective;
import com.xarchimedesx.templatesgenerator.reader.ContextVariablesReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
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
  private final ContextVariablesReader reader;
  private final Context toolContext;
  private final VelocityEngine engine;

  /**
   * CLI entrypoint. Parses input array of strings using Apache CLI.
   *
   * @param args input command line configuration.<br>
   *             --template (-t) - Velocity template path.<br>
   *             --variables (-v) - Comma-separated list of paths to files/directories with variables.
   *             .json, .yaml/.yml extensions are supported.<br>
   *             --output (-o) - Output path.<br>
   *             --combine (-c) - No args. Whether to combine multiple variables files' content inside single context
   *             or to render output per input file. Optional. 'False' if not set.
   */
  public static void main(String[] args) {
    CommandLine cli = new Parser().parse(args);

    String templatePath = cli.getOptionValue("template");
    String variablesPaths = cli.getOptionValue("variables");
    String outputDirBasePath = cli.getOptionValue("output");
    boolean isCombined = cli.hasOption("combine");

    TemplatesGenerator tg = new TemplatesGenerator();
    tg.render(templatePath, variablesPaths, outputDirBasePath, isCombined);
  }

  public TemplatesGenerator() {
    this.engine = initializeAndGetVelocityEngine();
    this.toolContext = initializeAndGetToolContext();
    this.reader = new ContextVariablesReader();
  }

  /**
   * Entrypoint when using Templates-Generator as a library.
   *
   * @param templatePath      Velocity template path.
   * @param variablesPaths    Comma-separated list of paths to files/directories with variables.
   *                          .json, .yaml/.yml extensions are supported.<br>
   * @param outputDirBasePath Output path.
   * @param isCombined        Whether to combine multiple variables files' content inside single context
   *                          or to render output per input file.
   */
  public void render(String templatePath, String variablesPaths, String outputDirBasePath, boolean isCombined) {
    List<String> parsedVariablesPaths = preprocessVariablesPaths(variablesPaths);
    templatePath = FilenameUtils.normalize(templatePath);
    outputDirBasePath = FilenameUtils.normalize(outputDirBasePath);

    LOGGER.info("Running rendering with\n    Velocity template path: {}\n    Variables files paths: {}\n    Output path: {}\n    Is combined: {}",
        templatePath, parsedVariablesPaths, outputDirBasePath, isCombined);

    Template template = engine.getTemplate(templatePath);
    List<Pair<String, Map<String, Object>>> variables = reader.getVariables(parsedVariablesPaths, isCombined);
    int filesToBeSaved = variables.size();

    for (Pair<String, Map<String, Object>> var : variables) {
      String outputPath = formOutputPath(filesToBeSaved, outputDirBasePath, var.getKey());
      Context velocityContext = initializeAndGetVelocityContext(var.getValue(), outputPath);
      mergeTemplateAndVelocityContext(template, velocityContext);
    }
  }

  private VelocityEngine initializeAndGetVelocityEngine() {
    VelocityEngine engine = new VelocityEngine();
    engine.setProperty(RuntimeConstants.CUSTOM_DIRECTIVES, SaveFileDirective.class.getName());
    engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, RESOURCE_LOADERS_NAMES);
    engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "");
    engine.setProperty(RuntimeConstants.EVENTHANDLER_INCLUDE, IncludeRelativePath.class.getName());
    engine.setProperty(FILE_RESOURCE_LOADER, FileResourceLoader.class.getName());
    engine.setProperty(CLASSPATH_RESOURCE_LOADER, ClasspathResourceLoader.class.getName());
    engine.init();

    return engine;
  }

  private Context initializeAndGetToolContext() {
    ToolManager toolManager = new ToolManager(false, true);
    toolManager.configure(TOOLS_CONFIG_FILE);
    toolManager.setVelocityEngine(engine);

    return toolManager.createContext();
  }

  private Context initializeAndGetVelocityContext(Map<String, Object> variables, String outputDirBasePath) {
    Context velocityContext = new VelocityContext(variables, toolContext);
    velocityContext.put(OUTPUT_DIR_BASE_PATH_REFERENCE_NAME, outputDirBasePath);

    return velocityContext;
  }

  private void mergeTemplateAndVelocityContext(Template template, Context velocityContext) {
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
    return filesToBeSaved == 1
        ? outputDirBasePath
        : String.join("/", outputDirBasePath, renderedFileSubpath);
  }
}
