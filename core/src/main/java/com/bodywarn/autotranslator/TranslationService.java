package com.bodywarn.autotranslator;

import com.bodywarn.autotranslator.settings.Language;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

public class TranslationService {

  private static final String[] LINGVA_INSTANCES = {
      "https://translate.plausibility.cloud/api/v1",
      "https://lingva.lunar.icu/api/v1",
      "https://lingva.garudalinux.org/api/v1"
  };

  private final ExecutorService executor = Executors.newFixedThreadPool(5);
  private final ConcurrentHashMap<String, String> translationCache = new ConcurrentHashMap<>();
  private static final int MAX_CACHE_SIZE = 1000;

  private static final Map<String, String[]> LANGUAGE_KEYWORDS = new HashMap<>();

  static {
    LANGUAGE_KEYWORDS.put("da", new String[]{
        "jeg", "mig", "dig", "hvordan", "også", "ikke", "med", "hej", "hvad", "dansk",
        "være", "havde", "ville", "kunne", "skulle", "måske", "altså", "godt", "går"
    });

    LANGUAGE_KEYWORDS.put("de", new String[]{
        "ich", "mich", "dich", "wie", "geht", "haben", "der", "die", "das", "ein",
        "eine", "nicht", "und", "aber", "mit", "von", "für", "bist", "deutsch", "heute"
    });

    LANGUAGE_KEYWORDS.put("sv", new String[]{
        "jag", "mig", "hur", "mår", "också", "inte", "med", "hej", "vad", "svenska",
        "vara", "hade", "skulle", "kanske", "bra", "går"
    });

    LANGUAGE_KEYWORDS.put("no", new String[]{
        "jeg", "meg", "deg", "hvordan", "også", "ikke", "med", "hei", "hva", "norsk",
        "være", "hadde", "ville", "kunne", "skulle", "kanskje", "bra", "går"
    });

    LANGUAGE_KEYWORDS.put("fr", new String[]{
        "je", "tu", "il", "elle", "nous", "vous", "ils", "elles", "est", "sont",
        "le", "la", "les", "un", "une", "de", "du", "et", "mais", "pour", "avec", "français"
    });

    LANGUAGE_KEYWORDS.put("es", new String[]{
        "yo", "tú", "él", "ella", "nosotros", "vosotros", "ellos", "ellas", "es", "son",
        "el", "la", "los", "las", "un", "una", "de", "del", "pero", "para", "español"
    });

    LANGUAGE_KEYWORDS.put("it", new String[]{
        "io", "tu", "lui", "lei", "noi", "voi", "loro", "è", "sono", "ha", "hanno",
        "il", "la", "gli", "le", "un", "una", "di", "del", "per", "italiano"
    });

    LANGUAGE_KEYWORDS.put("pt", new String[]{
        "eu", "você", "ele", "ela", "nós", "vós", "eles", "elas", "é", "são",
        "o", "a", "os", "as", "um", "uma", "de", "do", "mas", "para", "português"
    });

    LANGUAGE_KEYWORDS.put("en", new String[]{
        "the", "is", "are", "was", "were", "have", "has", "had", "will", "would",
        "could", "should", "can", "may", "might", "what", "how", "when", "where", "english"
    });
  }

  public TranslationService() {
    System.out.println("[AutoTranslator] Initialized with " + LINGVA_INSTANCES.length + " Lingva instances");
  }

  private String detectLanguage(String text) {
    if (text == null || text.trim().isEmpty()) {
      return "auto";
    }

    String lowerText = text.toLowerCase();
    Map<String, Integer> scores = new HashMap<>();

    for (String lang : LANGUAGE_KEYWORDS.keySet()) {
      scores.put(lang, 0);
    }

    for (Map.Entry<String, String[]> entry : LANGUAGE_KEYWORDS.entrySet()) {
      String lang = entry.getKey();
      String[] keywords = entry.getValue();

      for (String keyword : keywords) {
        String pattern = "\\b" + keyword + "\\b";
        if (lowerText.matches(".*" + pattern + ".*")) {
          scores.put(lang, scores.get(lang) + 1);
        }
      }
    }

    String detectedLang = "auto";
    int maxScore = 0;

    for (Map.Entry<String, Integer> entry : scores.entrySet()) {
      if (entry.getValue() > maxScore) {
        maxScore = entry.getValue();
        detectedLang = entry.getKey();
      }
    }

    if (maxScore == 0) {
      return "auto";
    }

    System.out.println("[AutoTranslator] Language detection: " + detectedLang + " (score: " + maxScore + ")");
    return detectedLang;
  }


  public CompletableFuture<String> translateAsync(String text, Language targetLang) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        if (text == null || text.trim().isEmpty()) {
          return text;
        }

        String langCode = getLanguageCode(targetLang);
        String cacheKey = text + "|" + langCode;

        if (translationCache.containsKey(cacheKey)) {
          System.out.println("[AutoTranslator] Cache hit for: " + text);
          return translationCache.get(cacheKey);
        }

        String detectedLang = detectLanguage(text);
        System.out.println("[AutoTranslator] Detected: " + detectedLang + ", Target: " + langCode);

        if (detectedLang.equals(langCode)) {
          System.out.println("[AutoTranslator] Already in target language");
          return text;
        }

        System.out.println("[AutoTranslator] Translating: " + text);
        String result = translate(text, detectedLang, langCode);
        System.out.println("[AutoTranslator] Result: " + result);

        if (result != null && translationCache.size() < MAX_CACHE_SIZE) {
          translationCache.put(cacheKey, result);
        }

        return result;

      } catch (Exception e) {
        System.err.println("[AutoTranslator] Translation failed: " + e.getMessage());
        e.printStackTrace();
        return text;
      }
    }, executor);
  }


  private String translate(String text, String sourceLang, String targetLang) throws Exception {
    if (text == null || text.trim().isEmpty()) {
      return text;
    }

    Exception lastException = null;

    for (int i = 0; i < LINGVA_INSTANCES.length; i++) {
      try {
        System.out.println("[AutoTranslator] Trying Lingva instance " + (i + 1));
        String result = translateWithLingva(text, sourceLang, targetLang, LINGVA_INSTANCES[i]);

        if (result != null && !result.trim().isEmpty() && !result.equals(text)) {
          System.out.println("[AutoTranslator] Success with instance " + (i + 1));
          return result;
        }
      } catch (Exception e) {
        System.err.println("[AutoTranslator] Instance " + (i + 1) + " failed: " + e.getMessage());
        lastException = e;
      }
    }

    try {
      System.out.println("[AutoTranslator] Trying Google Translate fallback");
      String result = translateWithGoogle(text, sourceLang, targetLang);
      if (result != null && !result.trim().isEmpty()) {
        return result;
      }
    } catch (Exception e) {
      System.err.println("[AutoTranslator] Google Translate failed: " + e.getMessage());
    }

    throw new Exception("All translation services failed. Last error: " +
        (lastException != null ? lastException.getMessage() : "Unknown error"));
  }

  private String translateWithLingva(String text, String sourceLanguage, String targetLanguage, String apiUrl) throws Exception {
    String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
    String urlString = apiUrl + "/" + sourceLanguage + "/" + targetLanguage + "/" + encodedText;

    System.out.println("[AutoTranslator] Lingva request: " + urlString);

    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
    conn.setConnectTimeout(3000); // Reduced timeout for speed
    conn.setReadTimeout(3000);

    int responseCode = conn.getResponseCode();
    System.out.println("[AutoTranslator] Lingva response code: " + responseCode);

    if (responseCode != 200) {
      conn.disconnect();
      throw new Exception("HTTP Error " + responseCode);
    }

    BufferedReader in = new BufferedReader(
        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder response = new StringBuilder();
    String inputLine;

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();
    conn.disconnect();

    String jsonResponse = response.toString();
    System.out.println("[AutoTranslator] Lingva response: " + jsonResponse);

    JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

    if (jsonObject.has("translation")) {
      String translation = jsonObject.get("translation").getAsString().trim();
      String decoded = URLDecoder.decode(translation.replace("+", " "), StandardCharsets.UTF_8.toString());
      return decoded;
    }

    throw new Exception("No translation in response");
  }

  private String translateWithGoogle(String text, String sourceLanguage, String targetLanguage) throws Exception {
    String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());

    String urlString = String.format(
        "https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s",
        sourceLanguage.equals("auto") ? "auto" : sourceLanguage,
        targetLanguage,
        encodedText
    );

    System.out.println("[AutoTranslator] Google Translate request");

    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
    conn.setConnectTimeout(3000);
    conn.setReadTimeout(3000);

    int responseCode = conn.getResponseCode();
    System.out.println("[AutoTranslator] Google Translate response code: " + responseCode);

    if (responseCode != 200) {
      conn.disconnect();
      throw new Exception("HTTP Error " + responseCode);
    }

    BufferedReader in = new BufferedReader(
        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder response = new StringBuilder();
    String inputLine;

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();
    conn.disconnect();

    String jsonResponse = response.toString();

    JsonArray jsonArray = JsonParser.parseString(jsonResponse).getAsJsonArray();
    if (jsonArray.size() > 0) {
      JsonArray translationArray = jsonArray.get(0).getAsJsonArray();
      if (translationArray.size() > 0) {
        JsonArray innerArray = translationArray.get(0).getAsJsonArray();
        if (innerArray.size() > 0) {
          String translation = innerArray.get(0).getAsString().trim();
          System.out.println("[AutoTranslator] Google Translate result: " + translation);
          return translation;
        }
      }
    }

    throw new Exception("No translation in Google response");
  }

  private String getLanguageCode(Language language) {
    switch (language) {
      case EN:
        return "en";
      case DA:
        return "da";
      case SV:
        return "sv";
      case NO:
        return "no";
      case DE:
        return "de";
      case FR:
        return "fr";
      case ES:
        return "es";
      case IT:
        return "it";
      case PT:
        return "pt";
      case ZH:
        return "zh";
      default:
        return "en";
    }
  }

  public void clearCache() {
    translationCache.clear();
    System.out.println("[AutoTranslator] Cache cleared");
  }

  public int getCacheSize() {
    return translationCache.size();
  }

  public void shutdown() {
    executor.shutdown();
    clearCache();
  }
}