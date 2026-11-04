#!/usr/bin/env python3
"""Store Resend SMTP settings in the ignored local environment file."""

from __future__ import annotations

import getpass
import os
import shlex
import tempfile
from pathlib import Path

ENV_FILE = Path(__file__).resolve().parents[1] / ".env.local"


def configure_env(path: Path, api_key: str) -> None:
    if not path.is_file():
        raise FileNotFoundError(f"No existe {path}. Configura primero el backend local.")
    if not api_key.startswith("re_") or len(api_key) < 12:
        raise ValueError("La clave no parece ser una API key de Resend (re_...).")

    settings = {
        "MAIL_HOST": "smtp.resend.com",
        "MAIL_PORT": "465",
        "MAIL_USERNAME": "resend",
        "MAIL_PASSWORD": api_key,
        "MAIL_SMTP_AUTH": "true",
        "MAIL_SMTP_SSL": "true",
        "MAIL_SMTP_STARTTLS": "false",
        "MAIL_FROM": "notificaciones@mail.se-estatus.com",
    }
    lines = []
    written = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        key = line.split("=", 1)[0].strip() if "=" in line else ""
        if key in settings and not line.lstrip().startswith("#"):
            if key not in written:
                lines.append(f"{key}={shlex.quote(settings[key])}")
                written.add(key)
            continue
        lines.append(line)
    for key, value in settings.items():
        if key not in written:
            lines.append(f"{key}={shlex.quote(value)}")

    fd, temporary = tempfile.mkstemp(prefix=".env.local.", dir=path.parent, text=True)
    try:
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, "w", encoding="utf-8") as output:
            output.write("\n".join(lines) + "\n")
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


if __name__ == "__main__":
    try:
        key = getpass.getpass("Pega tu clave de Resend (no se mostrará): ").strip()
        configure_env(ENV_FILE, key)
    except (FileNotFoundError, ValueError) as exc:
        raise SystemExit(str(exc)) from exc
    print("Resend configurado en .env.local. La clave permanece fuera de Git.")
