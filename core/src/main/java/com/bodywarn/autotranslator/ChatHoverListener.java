package com.bodywarn.autotranslator;

import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.chat.ChatReceiveEvent;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHoverListener {

  private final AutoTranslatorAddon addon;
  private final Map<String, String> translationCache = new ConcurrentHashMap<>();
  private final PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();

  public ChatHoverListener(AutoTranslatorAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onChatReceive(ChatReceiveEvent event) {
    if (!this.addon.configuration().enabled().get()) {
      return;
    }

    try {
      String originalMessage = event.chatMessage().getPlainText();

      if (originalMessage == null || originalMessage.trim().isEmpty()) {
        return;
      }

      String cleanedText = cleanMessage(originalMessage);

      this.addon.logger().info("Original: '" + originalMessage + "'");
      this.addon.logger().info("Cleaned: '" + cleanedText + "'");

      if (cleanedText.length() < 3) {
        this.addon.logger().info("Message too short after cleaning, skipping");
        return;
      }

      if (cleanedText.startsWith("/") || cleanedText.startsWith("!")) {
        return;
      }

      Component messageComponent = event.chatMessage().component();
      final String textToTranslate = cleanedText;

      this.addon.getTranslationService()
          .translateAsync(textToTranslate, this.addon.configuration().targetLanguage().get())
          .thenAccept(translated -> {
            if (translated != null && !translated.isEmpty()) {
              if (translated.equals(textToTranslate)) {
                this.addon.logger().info("Text is already in target language");
                addAlreadyTranslatedHover(messageComponent, textToTranslate);
              } else {
                this.addon.logger().info("Translation: '" + textToTranslate + "' -> '" + translated + "'");
                translationCache.put(textToTranslate, translated);
                addHoverToComponent(messageComponent, textToTranslate, translated);
              }
            } else {
              this.addon.logger().warn("Translation failed or returned empty text");
            }
          })
          .exceptionally(throwable -> {
            this.addon.logger().error("Translation error", throwable);
            return null;
          });
    } catch (Exception e) {
      this.addon.logger().error("Error in chat listener", e);
    }
  }

  private String cleanMessage(String message) {
    if (message == null) {
      return "";
    }

    String cleaned = message.trim();

    if (cleaned.contains(">")) {
      int index = cleaned.indexOf('>');
      if (index < cleaned.length() - 1) {
        cleaned = cleaned.substring(index + 1).trim();
      }
    } else if (cleaned.contains(":")) {
      int index = cleaned.indexOf(':');
      if (index < cleaned.length() - 1) {
        cleaned = cleaned.substring(index + 1).trim();
      }
    }

    cleaned = cleaned.replaceAll("§.", "");

    cleaned = cleaned.replaceAll("chat\\.type\\.\\w+", "");

    return cleaned.trim();
  }

  private void addHoverToComponent(Component component, String originalText, String translatedText) {
    try {
      if (component == null) {
        this.addon.logger().warn("Component is null, cannot add hover");
        return;
      }

      Component hoverComponent = Component.text()
          .append(Component.text("Translation:", NamedTextColor.GOLD))
          .append(Component.newline())
          .append(Component.text(translatedText, NamedTextColor.WHITE))
          .build();

      component.hoverEvent(HoverEvent.showText(hoverComponent));

      this.addon.logger().info("Hover added successfully");
    } catch (Exception e) {
      this.addon.logger().error("Error adding hover", e);
    }
  }

  private void addAlreadyTranslatedHover(Component component, String text) {
    try {
      if (component == null) {
        this.addon.logger().warn("Component is null, cannot add hover");
        return;
      }

      Component hoverComponent = Component.text()
          .append(Component.text("Already in target language", NamedTextColor.GREEN))
          .append(Component.newline())
          .append(Component.text("(No translation needed)", NamedTextColor.GRAY))
          .build();

      component.hoverEvent(HoverEvent.showText(hoverComponent));

      this.addon.logger().info("Already-translated hover added successfully");
    } catch (Exception e) {
      this.addon.logger().error("Error adding already-translated hover", e);
    }
  }

  public void clearCache() {
    this.translationCache.clear();
    this.addon.logger().info("Translation cache cleared");
  }
}