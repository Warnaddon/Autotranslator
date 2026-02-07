package com.bodywarn.autotranslator;

import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.ScreenRenderEvent;
import com.bodywarn.autotranslator.settings.TranslatorAddon;

public class TooltipRenderer {

  private final AutoTranslatorAddon addon;

  public TooltipRenderer(AutoTranslatorAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onScreenRender(ScreenRenderEvent event) {
    if (!this.addon.configuration().enabled().get()) {
      return;
    }

  }
}