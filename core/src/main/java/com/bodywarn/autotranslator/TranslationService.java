package com.bodywarn.autotranslator;

import com.bodywarn.autotranslator.settings.Language;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TranslationService {

  private static final String[] LINGVA_INSTANCES = {
      "https://translate.plausibility.cloud/api/v1",
      "https://lingva.lunar.icu/api/v1",
      "https://lingva.garudalinux.org/api/v1",
      "https://lingva.ml/api/v1"
  };

  private static final String SIMPLYTRANSLATE_API = "https://simplytranslate.org/api/translate";
  private static final String MYMEMORY_API = "https://api.mymemory.translated.net/get";

  private static final String LANGUAGETOOL_API = "https://api.languagetool.org/v2/check";

  private final ExecutorService executor = Executors.newFixedThreadPool(3);
  private final ConcurrentHashMap<String, String> translationCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> grammarCache = new ConcurrentHashMap<>();
  private static final int MAX_CACHE_SIZE = 1000;

  private static final Map<String, String> SLANG_DICTIONARY = new HashMap<>();
  static {
    SLANG_DICTIONARY.put("wyd", "what are you doing");
    SLANG_DICTIONARY.put("hru", "how are you");
    SLANG_DICTIONARY.put("hyd", "how are you doing");
    SLANG_DICTIONARY.put("wbu", "what about you");
    SLANG_DICTIONARY.put("brb", "be right back");
    SLANG_DICTIONARY.put("gtg", "got to go");
    SLANG_DICTIONARY.put("g2g", "got to go");
    SLANG_DICTIONARY.put("gg", "good game");
    SLANG_DICTIONARY.put("wp", "well played");
    SLANG_DICTIONARY.put("ty", "thank you");
    SLANG_DICTIONARY.put("thx", "thanks");
    SLANG_DICTIONARY.put("np", "no problem");
    SLANG_DICTIONARY.put("nvm", "never mind");
    SLANG_DICTIONARY.put("idk", "i don't know");
    SLANG_DICTIONARY.put("idc", "i don't care");
    SLANG_DICTIONARY.put("imo", "in my opinion");
    SLANG_DICTIONARY.put("imho", "in my humble opinion");
    SLANG_DICTIONARY.put("tbh", "to be honest");
    SLANG_DICTIONARY.put("btw", "by the way");
    SLANG_DICTIONARY.put("lol", "laughing out loud");
    SLANG_DICTIONARY.put("lmao", "laughing my ass off");
    SLANG_DICTIONARY.put("rofl", "rolling on floor laughing");
    SLANG_DICTIONARY.put("omg", "oh my god");
    SLANG_DICTIONARY.put("omw", "on my way");
    SLANG_DICTIONARY.put("afk", "away from keyboard");
    SLANG_DICTIONARY.put("jk", "just kidding");
    SLANG_DICTIONARY.put("irl", "in real life");
    SLANG_DICTIONARY.put("rn", "right now");
    SLANG_DICTIONARY.put("asap", "as soon as possible");
    SLANG_DICTIONARY.put("pls", "please");
    SLANG_DICTIONARY.put("plz", "please");
    SLANG_DICTIONARY.put("msg", "message");
    SLANG_DICTIONARY.put("sup", "what's up");
    SLANG_DICTIONARY.put("ur", "your");
    SLANG_DICTIONARY.put("u", "you");
    SLANG_DICTIONARY.put("r", "are");

    SLANG_DICTIONARY.put("si", "og");
    SLANG_DICTIONARY.put("ka", "kan");
    SLANG_DICTIONARY.put("d", "det");
    SLANG_DICTIONARY.put("s", "så");
    SLANG_DICTIONARY.put("ik", "ikke");
    SLANG_DICTIONARY.put("ikk", "ikke");
    SLANG_DICTIONARY.put("ike", "ikke");
    SLANG_DICTIONARY.put("ha", "har");
    SLANG_DICTIONARY.put("e", "er");
    SLANG_DICTIONARY.put("m", "med");
    SLANG_DICTIONARY.put("dg", "dig");
    SLANG_DICTIONARY.put("virkli", "virkelig");
    SLANG_DICTIONARY.put("virke", "virkelig");
    SLANG_DICTIONARY.put("nå", "nu");
    SLANG_DICTIONARY.put("me", "med");
    SLANG_DICTIONARY.put("hvad", "hvad");
    SLANG_DICTIONARY.put("hva", "hvad");
    SLANG_DICTIONARY.put("ska", "skal");
    SLANG_DICTIONARY.put("vi", "vil");
    SLANG_DICTIONARY.put("ba", "bare");

    SLANG_DICTIONARY.put("va", "vad");
    SLANG_DICTIONARY.put("nån", "någon");
    SLANG_DICTIONARY.put("nåt", "något");
    SLANG_DICTIONARY.put("å", "och");
    SLANG_DICTIONARY.put("asså", "alltså");

    SLANG_DICTIONARY.put("ikkje", "ikke");
    SLANG_DICTIONARY.put("koffor", "hvorfor");
    SLANG_DICTIONARY.put("korfor", "hvorfor");
    SLANG_DICTIONARY.put("koffer", "hvorfor");
    SLANG_DICTIONARY.put("kossen", "hvordan");
    SLANG_DICTIONARY.put("åssen", "hvordan");

    SLANG_DICTIONARY.put("nen", "einen");
    SLANG_DICTIONARY.put("ne", "eine");
    SLANG_DICTIONARY.put("hab", "habe");
    SLANG_DICTIONARY.put("haste", "hast du");
    SLANG_DICTIONARY.put("biste", "bist du");
    SLANG_DICTIONARY.put("nich", "nicht");
    SLANG_DICTIONARY.put("net", "nicht");

    SLANG_DICTIONARY.put("chui", "je suis");
    SLANG_DICTIONARY.put("jsuis", "je suis");
    SLANG_DICTIONARY.put("jpeux", "je peux");
    SLANG_DICTIONARY.put("jveux", "je veux");
    SLANG_DICTIONARY.put("jsais", "je sais");
    SLANG_DICTIONARY.put("jtm", "je t'aime");
    SLANG_DICTIONARY.put("mdr", "mort de rire");
    SLANG_DICTIONARY.put("ptdr", "pété de rire");
    SLANG_DICTIONARY.put("slt", "salut");
    SLANG_DICTIONARY.put("bjr", "bonjour");
    SLANG_DICTIONARY.put("bsr", "bonsoir");
    SLANG_DICTIONARY.put("stp", "s'il te plaît");
    SLANG_DICTIONARY.put("svp", "s'il vous plaît");
    SLANG_DICTIONARY.put("pk", "pourquoi");
    SLANG_DICTIONARY.put("pq", "pourquoi");
    SLANG_DICTIONARY.put("tkt", "t'inquiète");
    SLANG_DICTIONARY.put("osef", "on s'en fout");

    SLANG_DICTIONARY.put("q", "que");
    SLANG_DICTIONARY.put("xq", "porque");
    SLANG_DICTIONARY.put("pq", "porque");
    SLANG_DICTIONARY.put("xk", "porque");
    SLANG_DICTIONARY.put("tb", "también");
    SLANG_DICTIONARY.put("tmb", "también");
    SLANG_DICTIONARY.put("tbn", "también");
    SLANG_DICTIONARY.put("bn", "bien");
    SLANG_DICTIONARY.put("bb", "bebé");
    SLANG_DICTIONARY.put("tq", "te quiero");
    SLANG_DICTIONARY.put("ta", "está");
    SLANG_DICTIONARY.put("toy", "estoy");
    SLANG_DICTIONARY.put("tas", "estás");
    SLANG_DICTIONARY.put("k", "que");

    SLANG_DICTIONARY.put("nn", "non");
    SLANG_DICTIONARY.put("xke", "perché");
    SLANG_DICTIONARY.put("xché", "perché");
    SLANG_DICTIONARY.put("cmq", "comunque");
    SLANG_DICTIONARY.put("cm", "come");
    SLANG_DICTIONARY.put("ke", "che");
    SLANG_DICTIONARY.put("tt", "tutto");
    SLANG_DICTIONARY.put("tvb", "ti voglio bene");
    SLANG_DICTIONARY.put("tvtb", "ti voglio tanto bene");

    SLANG_DICTIONARY.put("vc", "você");
    SLANG_DICTIONARY.put("blz", "beleza");
    SLANG_DICTIONARY.put("flw", "falou");
    SLANG_DICTIONARY.put("vlw", "valeu");
    SLANG_DICTIONARY.put("td", "tudo");
    SLANG_DICTIONARY.put("hj", "hoje");
  }

  public TranslationService() {
    System.out.println("[AutoTranslator] ✓ Grammar correction enabled (LanguageTool)");
    System.out.println("[AutoTranslator] ✓ Loaded " + LINGVA_INSTANCES.length + " Lingva mirrors");
    System.out.println("[AutoTranslator] ✓ Slang dictionary: " + SLANG_DICTIONARY.size() + " entries");
    System.out.println("[AutoTranslator] ✓ Quality verification enabled");
  }

  public CompletableFuture<String> translateAsync(String text, Language targetLang) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        String langCode = getLanguageCode(targetLang);
        String cacheKey = text + "|" + langCode;

        if (translationCache.containsKey(cacheKey)) {
          System.out.println("[AutoTranslator] ✓ Cache hit: " + text);
          return translationCache.get(cacheKey);
        }

        System.out.println("[AutoTranslator] Translating: " + text);

        String expandedText = expandSlang(text);
        if (!expandedText.equals(text)) {
          System.out.println("[AutoTranslator] Expanded slang: " + text + " → " + expandedText);
        }

        String sourceLang = detectLanguage(expandedText);
        System.out.println("[AutoTranslator] Detected: " + sourceLang + " → Target: " + langCode);

        if (sourceLang.equals(langCode)) {
          System.out.println("[AutoTranslator] Already in target language");
          return text;
        }

        String correctedText = correctGrammar(expandedText, sourceLang);
        if (!correctedText.equals(expandedText)) {
          System.out.println("[AutoTranslator] ✓ Grammar corrected: " + expandedText + " → " + correctedText);
        }

        String result = translateWithQualityCheck(correctedText, sourceLang, langCode);
        System.out.println("[AutoTranslator] ✓ Final result: " + result);

        if (result != null && translationCache.size() < MAX_CACHE_SIZE) {
          translationCache.put(cacheKey, result);
        }

        return result;
      } catch (Exception e) {
        System.err.println("[AutoTranslator] ✗ Failed: " + e.getMessage());
        return text;
      }
    }, executor);
  }

  private String correctGrammar(String text, String language) {
    String cacheKey = text + "|" + language;
    if (grammarCache.containsKey(cacheKey)) {
      return grammarCache.get(cacheKey);
    }

    try {
      String params = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8.toString()) +
          "&language=" + language +
          "&enabledOnly=false";

      URL url = new URL(LANGUAGETOOL_API);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setRequestProperty("User-Agent", "Mozilla/5.0");
      conn.setDoOutput(true);
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);

      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = params.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();
      if (responseCode != 200) {
        System.out.println("[AutoTranslator] Grammar check failed: HTTP " + responseCode);
        return text;
      }

      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        response.append(line);
      }
      in.close();

      JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

      if (!json.has("matches")) {
        return text;
      }

      JsonArray matches = json.getAsJsonArray("matches");
      if (matches.size() == 0) {
        System.out.println("[AutoTranslator] ✓ No grammar errors detected");
        return text;
      }

      String corrected = text;
      List<JsonObject> matchList = new ArrayList<>();
      for (int i = 0; i < matches.size(); i++) {
        matchList.add(matches.get(i).getAsJsonObject());
      }

      matchList.sort((a, b) -> {
        int offsetA = a.get("offset").getAsInt();
        int offsetB = b.get("offset").getAsInt();
        return Integer.compare(offsetB, offsetA);
      });

      for (JsonObject match : matchList) {
        int offset = match.get("offset").getAsInt();
        int length = match.get("length").getAsInt();

        if (!match.has("replacements")) continue;
        JsonArray replacements = match.getAsJsonArray("replacements");
        if (replacements.size() == 0) continue;

        String replacement = replacements.get(0).getAsJsonObject().get("value").getAsString();

        String before = corrected.substring(0, offset);
        String after = corrected.substring(offset + length);
        corrected = before + replacement + after;
      }

      System.out.println("[AutoTranslator] ✓ Applied " + matchList.size() + " grammar corrections");

      if (grammarCache.size() < MAX_CACHE_SIZE) {
        grammarCache.put(cacheKey, corrected);
      }

      return corrected;

    } catch (Exception e) {
      System.out.println("[AutoTranslator] Grammar check error: " + e.getMessage());
      return text;
    }
  }


  private String translateWithQualityCheck(String text, String sourceLang, String targetLang) throws Exception {
    List<String> translations = new ArrayList<>();

    for (String instance : LINGVA_INSTANCES) {
      try {
        String result = translateWithLingva(text, sourceLang, targetLang, instance);
        if (result != null && !result.isEmpty() && isValidTranslation(result)) {
          translations.add(result);
          System.out.println("[AutoTranslator] ✓ Lingva: " + result);
        }
      } catch (Exception e) {
      }
    }

    try {
      String result = translateWithSimplify(text, sourceLang, targetLang);
      if (result != null && !result.isEmpty() && isValidTranslation(result)) {
        translations.add(result);
        System.out.println("[AutoTranslator] ✓ Simplify: " + result);
      }
    } catch (Exception e) {
    }

    try {
      String result = translateWithMyMemory(text, sourceLang, targetLang);
      if (result != null && !result.isEmpty() && isValidTranslation(result)) {
        translations.add(result);
        System.out.println("[AutoTranslator] ✓ MyMemory: " + result);
      }
    } catch (Exception e) {
    }

    if (translations.isEmpty()) {
      throw new Exception("All translation services failed");
    }

    return selectBestTranslation(translations);
  }


  private boolean isValidTranslation(String translation) {
    if (translation == null || translation.trim().isEmpty()) {
      return false;
    }

    if (translation.replaceAll("[^a-zA-ZæøåÆØÅäöÄÖüÜß]", "").length() < 2) {
      return false;
    }

    return true;
  }


  private String selectBestTranslation(List<String> translations) {
    if (translations.size() == 1) {
      return translations.get(0);
    }

    Map<String, Integer> frequency = new HashMap<>();
    for (String t : translations) {
      String normalized = t.toLowerCase().trim();
      frequency.put(normalized, frequency.getOrDefault(normalized, 0) + 1);
    }

    String best = translations.get(0);
    int maxCount = 0;

    for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
      if (entry.getValue() > maxCount) {
        maxCount = entry.getValue();
        for (String t : translations) {
          if (t.toLowerCase().trim().equals(entry.getKey())) {
            best = t;
            break;
          }
        }
      }
    }

    if (maxCount > 1) {
      System.out.println("[AutoTranslator] ✓ Consensus: " + best + " (" + maxCount + "/" + translations.size() + " sources agree)");
    }

    return best;
  }

  private String expandSlang(String text) {
    String[] words = text.toLowerCase().split("\\s+");
    StringBuilder expanded = new StringBuilder();

    for (int i = 0; i < words.length; i++) {
      String word = words[i].replaceAll("[.,!?;:]", "");

      if (SLANG_DICTIONARY.containsKey(word)) {
        expanded.append(SLANG_DICTIONARY.get(word));
      } else {
        expanded.append(words[i]);
      }

      if (i < words.length - 1) {
        expanded.append(" ");
      }
    }

    return expanded.toString();
  }

  private String detectLanguage(String text) {
    text = text.toLowerCase();

    if (text.matches(".*(\\b(jeg|dig|hvordan|også|ikke|hej)\\b).*")) {
      return "da";
    }

    if (text.matches(".*(\\b(you|are|what|how|the|this|that|doing)\\b).*")) {
      return "en";
    }

    if (text.matches(".*(\\b(ich|du|wie|geht|bist|hallo)\\b).*")) {
      return "de";
    }

    if (text.matches(".*(\\b(jag|hur|mår|hej)\\b).*")) {
      return "sv";
    }

    if (text.matches(".*(\\b(hvordan|hei)\\b).*")) {
      return "no";
    }

    if (text.matches(".*(\\b(je|tu|comment|bonjour)\\b).*")) {
      return "fr";
    }

    if (text.matches(".*(\\b(yo|tú|cómo|hola)\\b).*")) {
      return "es";
    }

    if (text.matches(".*(\\b(io|tu|come|ciao)\\b).*")) {
      return "it";
    }

    if (text.matches(".*(\\b(eu|tu|como|olá)\\b).*")) {
      return "pt";
    }

    return "auto";
  }


  private String translateWithLingva(String text, String sourceLang, String targetLang, String apiUrl) throws Exception {
    String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
    String urlString = apiUrl + "/" + sourceLang + "/" + targetLang + "/" + encodedText;

    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
    conn.setConnectTimeout(8000);
    conn.setReadTimeout(8000);

    int responseCode = conn.getResponseCode();
    if (responseCode != 200) {
      throw new Exception("HTTP " + responseCode);
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = in.readLine()) != null) {
      response.append(line);
    }
    in.close();

    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

    if (json.has("translation")) {
      String translation = json.get("translation").getAsString().trim();
      return translation.replace("+", " ");
    }

    throw new Exception("No translation in response");
  }

  private String translateWithSimplify(String text, String sourceLang, String targetLang) throws Exception {
    String params = "?engine=google&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8.toString()) +
        "&sl=" + sourceLang + "&tl=" + targetLang;

    URL url = new URL(SIMPLYTRANSLATE_API + params);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
    conn.setConnectTimeout(8000);
    conn.setReadTimeout(8000);

    int responseCode = conn.getResponseCode();
    if (responseCode != 200) {
      throw new Exception("HTTP " + responseCode);
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = in.readLine()) != null) {
      response.append(line);
    }
    in.close();

    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

    if (json.has("translated-text")) {
      return json.get("translated-text").getAsString().trim();
    }

    throw new Exception("No translation in response");
  }

  private String translateWithMyMemory(String text, String sourceLang, String targetLang) throws Exception {
    String params = "?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8.toString()) +
        "&langpair=" + sourceLang + "|" + targetLang;

    URL url = new URL(MYMEMORY_API + params);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
    conn.setConnectTimeout(8000);
    conn.setReadTimeout(8000);

    int responseCode = conn.getResponseCode();
    if (responseCode != 200) {
      throw new Exception("HTTP " + responseCode);
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = in.readLine()) != null) {
      response.append(line);
    }
    in.close();

    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

    if (json.has("responseData")) {
      JsonObject responseData = json.getAsJsonObject("responseData");
      if (responseData.has("translatedText")) {
        return responseData.get("translatedText").getAsString().trim();
      }
    }

    throw new Exception("No translation in response");
  }

  private String getLanguageCode(Language language) {
    switch (language) {
      case EN: return "en";
      case DA: return "da";
      case SV: return "sv";
      case NO: return "no";
      case DE: return "de";
      case FR: return "fr";
      case ES: return "es";
      case IT: return "it";
      case PT: return "pt";
      case ZH: return "zh";
      default: return "en";
    }
  }

  public void clearCache() {
    translationCache.clear();
    grammarCache.clear();
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