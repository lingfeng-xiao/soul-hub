import time
import shlex
from pathlib import Path

from remote_support import connect_client, load_env_file, load_remote_settings, run_command, run_docker_command


PRIMARY_IMAGE = "neo4j:5.25"
MIRROR_IMAGE = "docker.m.daocloud.io/library/neo4j:5.25"
CYPHER_SMOKE_QUERY = "RETURN 1 AS ok"


def ensure_neo4j_image(client, sudo_password: str) -> str:
    image_candidates = [PRIMARY_IMAGE, MIRROR_IMAGE]
    for image_ref in image_candidates:
        stdout, stderr, exit_code = run_docker_command(
            client,
            f"docker image inspect {shlex.quote(image_ref)}",
            sudo_password=sudo_password,
        )
        if exit_code == 0:
            if image_ref != PRIMARY_IMAGE:
                run_docker_command(
                    client,
                    f"docker tag {shlex.quote(image_ref)} {shlex.quote(PRIMARY_IMAGE)}",
                    sudo_password=sudo_password,
                )
            return PRIMARY_IMAGE
        if "No such object" in stderr:
            continue

    pull_attempts = [
        ("primary", PRIMARY_IMAGE),
        ("mirror", MIRROR_IMAGE),
    ]
    last_error = ""
    for label, image_ref in pull_attempts:
        stdout, stderr, exit_code = run_docker_command(
            client,
            f"docker pull {shlex.quote(image_ref)}",
            sudo_password=sudo_password,
        )
        print(f"[{label}] {stdout.strip()}")
        if stderr.strip():
            print(f"[{label} stderr]")
            print(stderr.strip())
        if exit_code == 0:
            if image_ref != PRIMARY_IMAGE:
                run_docker_command(
                    client,
                    f"docker tag {shlex.quote(image_ref)} {shlex.quote(PRIMARY_IMAGE)}",
                    sudo_password=sudo_password,
                )
            return PRIMARY_IMAGE
        last_error = stderr.strip() or stdout.strip() or f"exit={exit_code}"

    raise SystemExit(f"Unable to fetch Neo4j image from primary or mirror source: {last_error}")


def wait_for_cypher_shell(client, container_name: str, neo4j_password: str, sudo_password: str) -> None:
    query = shlex.quote(CYPHER_SMOKE_QUERY)
    password = shlex.quote(neo4j_password)
    command = (
        f"docker exec {shlex.quote(container_name)} "
        f"cypher-shell -u neo4j -p {password} {query}"
    )
    for _ in range(60):
        stdout, stderr, exit_code = run_command(client, command, sudo_password=sudo_password)
        if exit_code == 0:
            if stdout.strip():
                print(stdout.strip())
            return
        if stderr.strip():
            print(stderr.strip())
        time.sleep(5)
    raise SystemExit("Neo4j container started but cypher-shell did not become ready in time.")


def main() -> None:
    env_path = Path(__file__).resolve().parents[2] / ".local" / "remote-verification.env"
    settings = load_remote_settings(env_path)
    file_values = load_env_file(env_path)

    container_name = file_values.get("DBJ_REMOTE_NEO4J_CONTAINER", "digital-beings-neo4j-dev")
    bolt_port = file_values.get("DBJ_REMOTE_NEO4J_BOLT_PORT", "17687")
    http_port = file_values.get("DBJ_REMOTE_NEO4J_HTTP_PORT", "17474")
    neo4j_password = file_values.get("DBJ_REMOTE_NEO4J_PASSWORD")
    if not neo4j_password:
        raise SystemExit("Missing required setting: DBJ_REMOTE_NEO4J_PASSWORD")

    client = connect_client(settings)

    try:
        run_docker_command(
            client,
            f"docker rm -f {shlex.quote(container_name)}",
            sudo_password=settings.sudo_password,
        )
        image_ref = ensure_neo4j_image(client, settings.sudo_password)

        start_command = (
            "docker run -d "
            f"--name {shlex.quote(container_name)} "
            f"-p {shlex.quote(bolt_port)}:7687 -p {shlex.quote(http_port)}:7474 "
            "-e NEO4J_server_memory_pagecache_size=256M "
            "-e NEO4J_server_memory_heap_initial__size=256M "
            "-e NEO4J_server_memory_heap_max__size=512M "
            "-e NEO4J_ACCEPT_LICENSE_AGREEMENT=yes "
            f"-e NEO4J_AUTH=neo4j/{shlex.quote(neo4j_password)} "
            f"{shlex.quote(image_ref)}"
        )
        out, err, exit_code = run_docker_command(client, start_command, sudo_password=settings.sudo_password)
        print(out.strip())
        if err.strip():
            print("STDERR:")
            print(err.strip())
        if exit_code != 0:
            raise SystemExit(f"Failed to start remote Neo4j container, exit={exit_code}")

        time.sleep(5)
        wait_for_cypher_shell(client, container_name, neo4j_password, settings.sudo_password)

        out, err, _ = run_docker_command(
            client,
            f"docker ps --filter name={shlex.quote(container_name)} --format 'table {{{{.Names}}}}\\t{{{{.Status}}}}\\t{{{{.Ports}}}}'",
            sudo_password=settings.sudo_password,
        )
        print(out.strip())
        if err.strip():
            print("STDERR:")
            print(err.strip())

        print(f"BOLT_URI=bolt://{settings.host}:{bolt_port}")
        print("USERNAME=neo4j")
        print("PASSWORD=<redacted>")
        print(f"HTTP_URI=http://{settings.host}:{http_port}")
    finally:
        client.close()


if __name__ == "__main__":
    main()
