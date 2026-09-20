package com.zerotheabsolute.fieldemitters;

import net.minecraft.network.chat.Component;

/** Carries a translation to command clients while retaining readable script errors. */
final class FieldOperationException extends IllegalArgumentException {
  private final Component text;

  FieldOperationException(String key, Object... arguments) {
    this(Component.translatable("command.fieldemitters.error." + key, arguments));
  }

  private FieldOperationException(Component text) {
    super(text.getString());
    this.text = text;
  }

  Component text() { return text; }
}
