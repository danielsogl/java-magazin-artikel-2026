#!/usr/bin/env python3
"""Dummy-Exfil-Listener fuer die Demo. Laeuft lokal, speichert nichts dauerhaft,
sondern loggt nur, was ankommt. So wird sichtbar, OB Daten durchkommen.

    python3 attack/listener.py            # lauscht auf 127.0.0.1:9999
"""
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        n = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(n)
        print(f"[EXFIL EMPFANGEN] {self.path}  {n} bytes")
        print(body.decode("utf-8", "replace"))
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"ok")

    def log_message(self, format, *args):  # noqa: A002
        pass


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9999
    print(f"Dummy-Listener auf 127.0.0.1:{port} (Strg-C beendet)")
    HTTPServer(("127.0.0.1", port), Handler).serve_forever()
