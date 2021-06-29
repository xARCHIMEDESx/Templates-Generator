package com.xarchimedesx.templatesgenerator.exception;

import org.apache.velocity.exception.VelocityException;

public class RenderingException extends VelocityException {
  public RenderingException(String exceptionMessage) {
    super(exceptionMessage);
  }
}
