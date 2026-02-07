package com.bodywarn.autotranslator.settings;

public enum Language {
  EN("English"),
  DA("Danish"),
  SV("Swedish"),
  NO("Norwegian"),
  DE("German"),
  FR("French"),
  ES("Spanish"),
  IT("Italian"),
  PT("Portuguese"),
  ZH("Chinese");

  private final String displayName;

  Language(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}