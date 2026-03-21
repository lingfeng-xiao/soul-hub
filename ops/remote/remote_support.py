from __future__ import annotations

from dataclasses import dataclass
import os
from pathlib import Path

import paramiko


@dataclass(frozen=True)
class RemoteSettings:
    host: str
    username: str
    password: str
    sudo_password: str


def load_env_file(env_file: Path) -> dict[str, str]:
    if not env_file.exists():
        return {}
    values: dict[str, str] = {}
    for raw_line in env_file.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def resolve_setting(name: str, fallback: dict[str, str], *, required: bool = True) -> str:
    value = os.getenv(name) or fallback.get(name)
    if not value:
        if required:
            raise SystemExit(f"Missing required setting: {name}")
        return ""
    return value


def load_remote_settings(env_path: Path) -> RemoteSettings:
    file_values = load_env_file(env_path)
    password = resolve_setting("DBJ_REMOTE_PASSWORD", file_values)
    sudo_password = os.getenv("DBJ_REMOTE_SUDO_PASSWORD") or file_values.get("DBJ_REMOTE_SUDO_PASSWORD") or password
    return RemoteSettings(
        host=resolve_setting("DBJ_REMOTE_HOST", file_values),
        username=resolve_setting("DBJ_REMOTE_USERNAME", file_values),
        password=password,
        sudo_password=sudo_password,
    )


def connect_client(settings: RemoteSettings) -> paramiko.SSHClient:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        hostname=settings.host,
        username=settings.username,
        password=settings.password,
        timeout=20,
        banner_timeout=20,
        auth_timeout=20,
    )
    return client


def run_command(
    client: paramiko.SSHClient,
    command: str,
    *,
    sudo_password: str | None = None,
    use_sudo: bool = False,
    get_pty: bool | None = None,
) -> tuple[str, str, int]:
    remote_command = command
    if use_sudo:
        remote_command = f"sudo -S -p '' {command}"
    stdin, stdout, stderr = client.exec_command(remote_command, get_pty=get_pty if get_pty is not None else use_sudo)
    if use_sudo:
        if not sudo_password:
            raise SystemExit("sudo_password is required when use_sudo=True")
        stdin.write(f"{sudo_password}\n")
        stdin.flush()
        stdin.channel.shutdown_write()
    exit_code = stdout.channel.recv_exit_status()
    return (
        stdout.read().decode("utf-8", errors="replace"),
        stderr.read().decode("utf-8", errors="replace"),
        exit_code,
    )


def run_docker_command(
    client: paramiko.SSHClient,
    command: str,
    *,
    sudo_password: str,
) -> tuple[str, str, int]:
    stdout, stderr, exit_code = run_command(client, command, sudo_password=sudo_password)
    if exit_code == 0:
        return stdout, stderr, exit_code
    if "permission denied" not in stderr.lower():
        return stdout, stderr, exit_code
    return run_command(client, command, sudo_password=sudo_password, use_sudo=True)
