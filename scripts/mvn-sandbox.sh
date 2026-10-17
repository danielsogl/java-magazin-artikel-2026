#!/usr/bin/env bash
# Maven in Claudes OS-Sandbox: Maven ignoriert HTTPS_PROXY. Die Sandbox setzt
# HTTPS_PROXY=http://user:pass@localhost:port; daraus eine temporaere
# Maven-Settings-Datei mit <proxy> erzeugen (ohne Repo-Credentials) und per -s
# uebergeben. Die echte ~/.m2/settings.xml ist per denyRead gesperrt.
set -euo pipefail
[[ -z "${HTTPS_PROXY:-}" ]] && exec mvn "$@"
p="${HTTPS_PROXY#*://}"; p="${p%%/*}"
hostport="${p##*@}"; user=""; pass=""
if [[ "$p" == *@* ]]; then cred="${p%@*}"; user="${cred%%:*}"; pass="${cred#*:}"; fi
mkdir -p target; cfg="$(mktemp target/.mvn-proxy.XXXXXX)"; trap 'rm -f "$cfg"' EXIT
cat > "$cfg" <<XML
<settings><proxies>
  <proxy><id>sandbox</id><active>true</active><protocol>https</protocol>
    <host>${hostport%:*}</host><port>${hostport##*:}</port>
    <username>$user</username><password>$pass</password></proxy>
  <proxy><id>sandbox-http</id><active>true</active><protocol>http</protocol>
    <host>${hostport%:*}</host><port>${hostport##*:}</port>
    <username>$user</username><password>$pass</password></proxy>
</proxies></settings>
XML
mvn -s "$cfg" "$@"
