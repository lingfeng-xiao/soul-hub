from pathlib import Path

from remote_support import connect_client, load_remote_settings, run_command


def main() -> None:
    env_path = Path(__file__).resolve().parents[2] / ".local" / "remote-verification.env"
    settings = load_remote_settings(env_path)

    command = (
        "uname -a; "
        "whoami; "
        "pwd; "
        "command -v docker || true; "
        "docker --version || true; "
        "docker compose version || true; "
        "java -version || true; "
        "groups; "
        "id; "
        "docker ps --format 'table {{.Names}}\\t{{.Status}}\\t{{.Ports}}' || true"
    )

    client = connect_client(settings)
    try:
        stdout, stderr, _ = run_command(client, command)
        print(stdout)
        err = stderr
        if err:
            print("STDERR:")
            print(err)
        sudo_stdout, sudo_stderr, sudo_exit = run_command(
            client,
            "true",
            sudo_password=settings.sudo_password,
            use_sudo=True,
        )
        if sudo_stdout.strip():
            print(sudo_stdout.strip())
        if sudo_stderr.strip():
            print("SUDO STDERR:")
            print(sudo_stderr.strip())
        print(f"SUDO_EXIT:{sudo_exit}")
    finally:
        client.close()


if __name__ == "__main__":
    main()
