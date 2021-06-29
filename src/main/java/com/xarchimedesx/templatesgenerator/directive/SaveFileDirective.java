package com.xarchimedesx.templatesgenerator.directive;

import com.xarchimedesx.templatesgenerator.exception.RenderingException;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.ASTBlock;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SaveFileDirective extends Directive {

  private static final String DIRECTIVE_NAME = "saveFile";

  @Override
  public String getName() {
    return DIRECTIVE_NAME;
  }

  @Override
  public int getType() {
    return BLOCK;
  }

  @Override
  public boolean render(InternalContextAdapter context, Writer writer, Node node) {
    try (StringWriter blockContent = new StringWriter()) {
      /*
       Checking whether the node object has two children and the second one is a directive block content.
       Then the first child is the path for file to be saved to.
       The second one (block content) is rendered to actual file content.
      */
      if (node.jjtGetNumChildren() == 2 && node.jjtGetChild(1) instanceof ASTBlock) {
        Path outputFilePath = Paths.get(String.valueOf(node.jjtGetChild(0).value(context)));
        node.jjtGetChild(1).render(context, blockContent);
        saveFileContent(blockContent.toString(), outputFilePath);
        return true;
      } else {
        throw new RenderingException("The #saveFile directive requires one argument - outputFilePath!");
      }
    } catch (IOException ioe) {
      log.error("A problem occurred while saving rendered file!", ioe);
      return false;
    }
  }

  private void saveFileContent(String fileContent, Path outputFilePath) throws IOException {
    log.info("Saving rendered file: {}", outputFilePath);
    if (!outputFilePath.toFile().exists()) {
      Files.createDirectories(outputFilePath);
    }
    Files.deleteIfExists(outputFilePath);
    Files.write(outputFilePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
  }
}
