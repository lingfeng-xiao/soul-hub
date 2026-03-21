from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

from remote_support import load_env_file, load_remote_settings


def build_env(env_path: Path) -> dict[str, str]:
    file_values = load_env_file(env_path)
    settings = load_remote_settings(env_path)
    merged = os.environ.copy()
    merged.setdefault("DIGITAL_BEINGS_NEO4J_URI", f"bolt://{settings.host}:17687")
    merged.setdefault("DIGITAL_BEINGS_NEO4J_USERNAME", "neo4j")
    merged.setdefault("DIGITAL_BEINGS_NEO4J_PASSWORD", file_values.get("DBJ_REMOTE_NEO4J_PASSWORD", ""))
    merged.setdefault("DIGITAL_BEINGS_SMOKE_ACTOR", "neo4j-smoke")
    return merged


def main() -> None:
    repo_root = Path(__file__).resolve().parents[2]
    env_path = repo_root / ".local" / "remote-verification.env"
    env = build_env(env_path)

    tests = "com.openclaw.digitalbeings.boot.DigitalBeingsNeo4jSmokeIT"
    command = [str(repo_root / "gradlew.bat"), ":boot-app:test", f"--tests={tests}"]

    print(f"Running: {' '.join(command)}")
    completed = subprocess.run(command, cwd=repo_root, env=env, check=False)
    raise SystemExit(completed.returncode)


if __name__ == "__main__":
    main()
