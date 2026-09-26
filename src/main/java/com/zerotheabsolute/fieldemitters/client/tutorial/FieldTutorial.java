package com.zerotheabsolute.fieldemitters.client.tutorial;

import com.zeromods.core.client.TutorialScreen;
import com.zeromods.core.tutorial.TutorialLesson;
import com.zeromods.core.tutorial.TutorialScene;
import com.zeromods.core.ui.UiTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FieldTutorial {
  private static final int STEP_COUNT = 4;
  private static final double MIN_STEP_SECONDS = 4;
  private static final double READING_WORDS_PER_SECOND = 4;
  private static final double READING_CJK_CHARACTERS_PER_SECOND = 8;
  private static final double READING_SETTLE_SECONDS = .5;
  private static final double CLAUSE_PAUSE_SECONDS = .25;
  private static final double MAX_CLAUSE_PAUSE_SECONDS = 1;
  private static final double TIMING_INCREMENT_SECONDS = .5;
  static final UiTheme THEME = new UiTheme(0xF00B1D2B, 0xFF102B3B, 0xFF355669,
      0xFFE2F5FF, 0xFF9DB5C5, 0xFF00D9ED, 0xFF54E5A5, 0xFFFFC76A, 0xFFFF6B7C);
  private static final String[] CHAPTERS = {"setup", "rails", "towers", "terrain", "equipment", "networks", "blocking", "directions", "age", "items", "cards", "checkpoint", "sensor", "damage", "appearance", "formations", "automation", "trouble"};

  private FieldTutorial() {}

  private static int controlsBeforeStep(String chapter) {
    return switch (chapter) {
      case "setup", "rails" -> 2;
      case "towers" -> 1;
      case "blocking", "directions", "sensor", "damage", "cards", "checkpoint",
          "appearance", "formations", "age", "items" -> 0;
      default -> -1;
    };
  }

  public static Component text(String key) {
    return Component.translatable("screen.fieldemitters.guide." + key);
  }

  private static double readingSeconds(Component caption, double animationSeconds) {
    String value = caption.getString();
    long cjk = value.codePoints().filter(code -> {
      var script = Character.UnicodeScript.of(code);
      return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
          || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL;
    }).count();
    long words = java.util.Arrays.stream(value.trim().split("\\s+"))
        .filter(word -> word.codePoints().anyMatch(code -> Character.UnicodeScript.of(code) == Character.UnicodeScript.LATIN
            || Character.UnicodeScript.of(code) == Character.UnicodeScript.CYRILLIC))
        .count();
    double reading = words / READING_WORDS_PER_SECOND + cjk / READING_CJK_CHARACTERS_PER_SECOND;
    long breaks = value.codePoints().filter(code -> ",;:.!?、，；：。！？".indexOf(code) >= 0).count();
    double pause = Math.min(MAX_CLAUSE_PAUSE_SECONDS, Math.max(0, breaks - 1) * CLAUSE_PAUSE_SECONDS);
    double seconds = Math.max(Math.max(MIN_STEP_SECONDS, animationSeconds), reading + READING_SETTLE_SECONDS + pause);
    return Math.ceil(seconds / TIMING_INCREMENT_SECONDS) * TIMING_INCREMENT_SECONDS;
  }

  public static void open(Screen parent) { open(parent, "setup"); }

  public static void open(Screen parent, String chapterId) {
    int chapterIndex = List.of(CHAPTERS).indexOf(chapterId);
    if (chapterIndex < 0) throw new IllegalArgumentException("Unknown tutorial chapter: " + chapterId);
    var scenes = new ArrayList<TutorialScene<GuiGraphics, Component>>();
    var renderer = new FieldTutorialScenes();
    for (String chapter : CHAPTERS) scenes.add(new Chapter(chapter, renderer));
    Minecraft.getInstance().setScreen(new TutorialScreen(parent, text("title"), Component.empty(),
        new TutorialLesson<>("fieldemitters:guide", 1, scenes), THEME, chapterIndex));
  }

  private record Segment(int step, boolean controls, Component caption, double start, double duration) {}

  private static final class Chapter implements TutorialScene<GuiGraphics, Component> {
    private final String name;
    private final FieldTutorialScenes renderer;
    private final List<Segment> segments;
    private final double duration;

    Chapter(String name, FieldTutorialScenes renderer) {
      this.name = name;
      this.renderer = renderer;
      var sequence = new ArrayList<Segment>();
      double start = 0;
      for (int step = 0; step < STEP_COUNT; step++) {
        if (step == controlsBeforeStep(name)) {
          Component caption = text("walkthrough." + name);
          double seconds = readingSeconds(caption, com.zerotheabsolute.fieldemitters.client.TutorialTunerPreview.minimumSeconds());
          sequence.add(new Segment(step, true, caption, start, seconds));
          start += seconds;
        }
        Component caption = text(name + ".step" + (step + 1));
        double seconds = readingSeconds(caption, FieldTutorialScenes.minimumSeconds(name, step));
        sequence.add(new Segment(step, false, caption, start, seconds));
        start += seconds;
      }
      segments = List.copyOf(sequence);
      duration = start;
    }

    private Segment segment(double seconds) {
      for (var segment : segments)
        if (seconds < segment.start() + segment.duration()) return segment;
      return segments.get(segments.size() - 1);
    }

    public String id() { return "fieldemitters:" + name; }
    public Component title() { return text(name + ".title"); }
    public double durationSeconds() { return duration; }
    public Component caption(double seconds) { return segment(seconds).caption(); }
    public List<Component> captions() { return segments.stream().map(Segment::caption).toList(); }
    public void render(GuiGraphics graphics, double seconds) {
      var current = segment(seconds);
      renderer.render(graphics, name, current.step(), Math.max(0, seconds - current.start()), current.controls());
    }
  }
}
