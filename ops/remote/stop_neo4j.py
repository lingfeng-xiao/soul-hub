from pathlib import Path

from remote_support import connect_client, load_remote_settings, run_docker_command


def main() -> None:
    env_path = Path(__file__).resolve().parents[2] / ".local" / "remote-verification.env"
    settings = load_remote_settings(env_path)
    file_values = {}
    if env_path.exists():
        from remote_support import load_env_file

        file_values = load_env_file(env_path)
    container_name = file_values.get("DBJ_REMOTE_NEO4J_CONTAINER", "digital-beings-neo4j-dev")

    client = connect_client(settings)

    try:
        stdout, stderr, exit_code = run_docker_command(
            client,
            f"docker rm -f {container_name} || true",
            sudo_password=settings.sudo_password,
        )
        print(stdout)
        err = stderr
        if err.strip():
            print("STDERR:")
            print(err.strip())
        if exit_code not in (0,):
            raise SystemExit(f"Remote stop command exited with {exit_code}")
    finally:
        client.close()


if __name__ == "__main__":
    main()
