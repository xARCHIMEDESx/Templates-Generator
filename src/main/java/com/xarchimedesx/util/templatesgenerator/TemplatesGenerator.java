package com.xarchimedesx.util.templatesgenerator;

import com.xarchimedesx.util.templatesgenerator.cli.Parser;
import com.xarchimedesx.util.templatesgenerator.directive.SaveFileDirective;
import com.xarchimedesx.util.templatesgenerator.reader.ContextVariablesReader;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
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

public class TemplatesGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(TemplatesGenerator.class);
  private static final String RESOURCE_LOADERS_NAMES = "file,classpath";
  private static final String FILE_RESOURCE_LOADER = "resource.loader.file.class";
  private static final String CLASSPATH_RESOURCE_LOADER = "resource.loader.classpath.class";
  private static final String TOOLS_CONFIG_FILE = "toolsConfiguration.xml";
  private static final String OUTPUT_DIR_BASE_PATH_REFERENCE_NAME = "outputDirBasePath";

  public static void main(String[] args) {
    new TemplatesGenerator(args);
  }

  public TemplatesGenerator(String[] args) {

    CommandLine cli = new Parser().parse(args);

    String templatePath = FilenameUtils.normalize(cli.getOptionValue("template"));
    String variablesPath = FilenameUtils.normalize(cli.getOptionValue("variables"));
    String outputDirBasePath = FilenameUtils.normalize(cli.getOptionValue("output"));

    LOGGER.info("Running MultiPathFileGenerator with\n    Velocity template path: {}\n    Variables file path: {}\n    Output path: {}",
        templatePath, variablesPath, outputDirBasePath);

    Map<String, Object> variables = new ContextVariablesReader().readVariables(variablesPath);

    VelocityEngine engine = initializeAndGetVelocityEngine(FilenameUtils.getFullPath(templatePath));
    Template template = engine.getTemplate(FilenameUtils.getName(templatePath));
    Context toolContext = initializeAndGetToolContext(engine);
    Context velocityContext = initializeAndGetVelocityContext(variables, toolContext, outputDirBasePath);

    mergeTemplateAndVelocityContext(template, velocityContext);
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

  private Context initializeAndGetVelocityContext(Map<String, Object> variables, Context toolContext, String outputDirBasePath) {
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
}
