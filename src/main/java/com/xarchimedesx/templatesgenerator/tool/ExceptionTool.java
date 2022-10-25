package com.xarchimedesx.templatesgenerator.tool;

import com.xarchimedesx.templatesgenerator.exception.RenderingException;
import org.apache.velocity.tools.config.DefaultKey;

@DefaultKey("exception")
public class ExceptionTool {
  public void throwRenderingException(String exceptionMessage) {
    throw new RenderingException(exceptionMessage);
  }
}
