"""Serve the built Android APK without repackaging it as a ZIP archive."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import os
import shutil


APK = Path(__file__).resolve().parents[1] / "app/build/outputs/apk/debug/app-debug.apk"
DOWNLOAD_PATH = "/MP3-Quran-Preview-Speed-Fix.apk"
PREVIOUS_DOWNLOAD_PATHS = (
    "/MP3-Quran-Preview-Playback-Fix.apk",
    "/MP3-Quran-Preview-Layout-Fix.apk",
    "/MP3-Quran-Preview.apk",
)
PAGE = """<!doctype html>
<html lang="bn">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>MP3 Quran Preview</title>
<style>
body { background:#071a14; color:#e9f3ee; font: 18px/1.6 system-ui, sans-serif;
       max-width:520px; margin:12vh auto; padding:24px; }
h1 { margin-bottom:8px; } p { color:#b5cac1; }
a { display:inline-block; background:#d4af37; color:#071a14; font-weight:700;
    text-decoration:none; padding:14px 22px; border-radius:12px; }
</style>
<h1>MP3 Quran Preview</h1>
<p>APK ফাইলটি সরাসরি ডাউনলোড করুন। পুরোনো MP3 Quran অ্যাপটি আনইনস্টল করতে হবে না।</p>
<a href="/MP3-Quran-Preview-Speed-Fix.apk" download="MP3-Quran-Preview-Speed-Fix.apk">APK ডাউনলোড করুন</a>
</html>"""


class Handler(BaseHTTPRequestHandler):
    def do_HEAD(self):
        self.respond(send_body=False)

    def do_GET(self):
        self.respond(send_body=True)

    def respond(self, send_body):
        if self.path == "/favicon.ico":
            self.send_response(204)
            self.end_headers()
        elif self.path == "/":
            body = PAGE.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            if send_body:
                self.wfile.write(body)
        elif self.path == DOWNLOAD_PATH or self.path in PREVIOUS_DOWNLOAD_PATHS:
            if not APK.is_file():
                self.send_error(503, "Preview APK has not been built yet")
                return
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header(
                "Content-Disposition",
                f'attachment; filename="{self.path[1:]}"'
            )
            self.send_header("Content-Length", str(APK.stat().st_size))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            if send_body:
                with APK.open("rb") as apk:
                    shutil.copyfileobj(apk, self.wfile)
        else:
            self.send_error(404)


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", int(os.environ.get("PORT", "5000"))), Handler).serve_forever()