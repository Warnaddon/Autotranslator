package com.bodywarn.autotranslator;

import com.bodywarn.autotranslator.settings.TranslatorAddon;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class AutoTranslatorAddon extends LabyAddon<TranslatorAddon> {

  private TranslationService translationService;
  private ChatHoverListener chatHoverListener;

  @Override
  protected void enable() {
    this.registerSettingCategory();

    this.translationService = new TranslationService();

    this.chatHoverListener = new ChatHoverListener(this);
    this.registerListener(this.chatHoverListener);

    this.logger().info("Auto Translator Addon enabled!");
    this.logger().info("Hover over chat messages to see translations!");
  }

  @Override
  protected Class<TranslatorAddon> configurationClass() {
    return TranslatorAddon.class;
  }



  public TranslationService getTranslationService() {
    return this.translationService;
  }

  public ChatHoverListener getChatHoverListener() {
    return this.chatHoverListener;
  }
}