///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.22.3

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PreToolUse-Guard fuer Claude Code (Listing 3).
 *
 * Liest das PreToolUse-Event als JSON von stdin, prueft Tool und Argumente
 * gegen eine Deny-Liste und blockiert mit Exit-Code 2 (stderr = Begruendung,
 * die Claude zu sehen bekommt). Ohne onFailure blockiert nur Exit 2 (bzw. JSON
 * permissionDecision "deny"); jeder andere Fehler laesst den Aufruf durch.
 * Deshalb steht in settings.json "onFailure": "block": dann blockiert jeder
 * Exit-Code ausser 0 -- auch 1, ungueltiges JSON oder ein Timeout.
 *
 * Zweite Verteidigungslinie HINTER den permissions aus settings.json: faengt,
 * was eine reine Glob-Regel nicht sieht (z. B. Secret-Pfade, die ueber ein
 * umgeleitetes HOME oder per cat/base64 in einem Bash-Kommando stecken).
 */
public class GuardHook {

    // Exit 2 blockiert den Tool-Aufruf; stderr wird Claude als Grund gezeigt.
    private static final int BLOCK = 2;

    // Dateinamen, die nie gelesen/ausgegeben werden sollen (auch nicht via Bash).
    private static final List<Pattern> SECRET_FILES = List.of(
            Pattern.compile("(^|/)settings\\.xml$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(^|/)gradle\\.properties$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(^|/)\\.env(\\..*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(^|/)\\.m2/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(^|/)\\.npmrc$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("id_(rsa|ed25519)", Pattern.CASE_INSENSITIVE));

    // Verdaechtige Bausteine in Bash-Kommandos (Exfiltration / Deploy).
    private static final List<Pattern> BASH_PATTERNS = List.of(
            Pattern.compile("\\bcurl\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwget\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bnc\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("mvn\\s+deploy", Pattern.CASE_INSENSITIVE),
            Pattern.compile("gradlew?\\s+publish", Pattern.CASE_INSENSITIVE),
            Pattern.compile("settings\\.xml", Pattern.CASE_INSENSITIVE));

    public static void main(String[] args) throws IOException {
        byte[] raw = System.in.readAllBytes();
        if (raw.length == 0) {
            System.exit(0); // kein Event -> nichts zu pruefen
        }

        JsonNode event = new ObjectMapper().readTree(raw);
        String tool = event.path("tool_name").asText("");
        JsonNode input = event.path("tool_input");

        switch (tool) {
            case "Read" -> {
                String path = input.path("file_path").asText("");
                block(SECRET_FILES, path,
                        "GuardHook: Lesen einer geschuetzten Datei verweigert: " + path);
            }
            case "Bash" -> {
                String cmd = input.path("command").asText("");
                block(BASH_PATTERNS, cmd,
                        "GuardHook: Bash-Kommando enthaelt ein blockiertes Muster: " + firstLine(cmd));
            }
            default -> { /* andere Tools interessieren diesen Hook nicht */ }
        }

        System.exit(0); // nichts getroffen -> normaler Permission-Flow
    }

    private static void block(List<Pattern> patterns, String value, String reason) {
        for (Pattern p : patterns) {
            if (p.matcher(value).find()) {
                System.err.println(reason);
                System.err.println("  (getroffenes Muster: " + p.pattern() + ")");
                System.exit(BLOCK);
            }
        }
    }

    private static String firstLine(String s) {
        int nl = s.indexOf('\n');
        return nl < 0 ? s : s.substring(0, nl) + " ...";
    }
}
