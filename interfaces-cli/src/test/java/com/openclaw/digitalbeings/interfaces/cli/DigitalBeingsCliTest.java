package com.openclaw.digitalbeings.interfaces.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class DigitalBeingsCliTest {

    @Test
    void shouldCreateAndListBeings() {
        CliRuntime runtime = CliRuntime.memory();

        CommandResult create = execute(runtime, "being", "create", "--display-name", "Aurora", "--actor", "tester");
        assertThat(create.exitCode()).isZero();
        String beingId = extractRequiredValue(create.stdout(), "beingId");

        CommandResult list = execute(runtime, "being", "list");
        assertThat(list.exitCode()).isZero();
        assertThat(list.stdout()).contains("displayName=Aurora");
        assertThat(list.stdout()).contains("beingId=" + beingId);
    }

    @Test
    void shouldManageLeaseLifecycle() {
        CliRuntime runtime = CliRuntime.memory();

        String beingId = extractRequiredValue(
                execute(runtime, "being", "create", "--display-name", "Atlas", "--actor", "tester").stdout(),
                "beingId"
        );

        String sessionId = extractRequiredValue(
                execute(runtime, "lease", "register-session", "--being-id", beingId, "--host-type", "workstation", "--actor", "tester").stdout(),
                "sessionId"
        );

        String leaseId = extractRequiredValue(
                execute(runtime, "lease", "acquire", "--being-id", beingId, "--session-id", sessionId, "--actor", "tester").stdout(),
                "leaseId"
        );

        CommandResult release = execute(runtime, "lease", "release", "--being-id", beingId, "--lease-id", leaseId, "--actor", "tester");
        assertThat(release.exitCode()).isZero();
        assertThat(release.stdout()).contains("status=RELEASED");
        assertThat(release.stdout()).contains("leaseId=" + leaseId);
        assertThat(release.stdout()).contains("sessionId=" + sessionId);
    }

    @Test
    void shouldManageReviewAndProjectionLifecycle() {
        CliRuntime runtime = CliRuntime.memory();

        String beingId = extractRequiredValue(
                execute(runtime, "being", "create", "--display-name", "Nova", "--actor", "tester").stdout(),
                "beingId"
        );

        String reviewItemId = extractRequiredValue(
                execute(
                        runtime,
                        "review",
                        "draft",
                        "--being-id", beingId,
                        "--lane", "governance",
                        "--kind", "policy",
                        "--proposal", "Allow release flow",
                        "--actor", "tester"
                ).stdout(),
                "reviewItemId"
        );

        assertThat(execute(runtime, "review", "submit", "--being-id", beingId, "--review-item-id", reviewItemId, "--actor", "tester").exitCode()).isZero();
        assertThat(
                execute(runtime, "review", "decide", "--being-id", beingId, "--review-item-id", reviewItemId, "--decision", "ACCEPTED", "--actor", "tester").stdout()
        ).contains("status=ACCEPTED");

        CommandResult rebuild = execute(runtime, "projection", "rebuild", "--being-id", beingId, "--actor", "tester");
        assertThat(rebuild.exitCode()).isZero();
        assertThat(rebuild.stdout()).contains("version=1");
        assertThat(rebuild.stdout()).contains("acceptedReviewItemIds=[" + reviewItemId + "]");

        CommandResult read = execute(runtime, "projection", "read", "--being-id", beingId);
        assertThat(read.exitCode()).isZero();
        assertThat(read.stdout()).contains("projectionId=");
        assertThat(read.stdout()).contains("contentSummary=" + reviewItemId);
    }

    @Test
    void shouldManageRelationshipLifecycle() {
        CliRuntime runtime = CliRuntime.memory();

        String beingId = extractRequiredValue(
                execute(runtime, "being", "create", "--display-name", "Orion", "--actor", "tester").stdout(),
                "beingId"
        );

        CommandResult create = execute(
                runtime,
                "relationship",
                "create",
                "--being-id", beingId,
                "--kind", "friend",
                "--display-name", "lingfeng",
                "--actor", "tester"
        );
        assertThat(create.exitCode()).isZero();
        assertThat(create.stdout()).contains("relationshipEntityId=");
        assertThat(execute(runtime, "relationship", "list", "--being-id", beingId).stdout())
                .contains("displayName=lingfeng");
    }

    @Test
    void shouldManageHostContractLifecycle() {
        CliRuntime runtime = CliRuntime.memory();

        String beingId = extractRequiredValue(
                execute(runtime, "being", "create", "--display-name", "Lyra", "--actor", "tester").stdout(),
                "beingId"
        );

        CommandResult create = execute(
                runtime,
                "host-contract",
                "create",
                "--being-id", beingId,
                "--host-type", "codex",
                "--actor", "tester"
        );
        assertThat(create.exitCode()).isZero();
        assertThat(create.stdout()).contains("contractId=");
        assertThat(execute(runtime, "host-contract", "list", "--being-id", beingId).stdout())
                .contains("hostType=codex");
    }

    @Test
    void shouldManageSnapshotLifecycle() {
        CliRuntime runtime = CliRuntime.memory();

        String beingId = extractRequiredValue(
                execute(runtime, "being", "create", "--display-name", "Helios", "--actor", "tester").stdout(),
                "beingId"
        );

        CommandResult create = execute(
                runtime,
                "snapshot",
                "create",
                "--being-id", beingId,
                "--type", "MILESTONE",
                "--summary", "phase-4 readiness checkpoint",
                "--actor", "tester"
        );
        assertThat(create.exitCode()).isZero();
        assertThat(create.stdout()).contains("snapshotId=");
        assertThat(execute(runtime, "snapshot", "list", "--being-id", beingId).stdout())
                .contains("summary=phase-4 readiness checkpoint");
    }

    @Test
    void shouldManageOwnerProfileFacts() {
        CliRuntime runtime = CliRuntime.memory();

        String beingId = extractRequiredValue(
                execute(runtime, "being", "create", "--display-name", "Iris", "--actor", "tester").stdout(),
                "beingId"
        );

        CommandResult record = execute(
                runtime,
                "owner-profile",
                "record",
                "--being-id", beingId,
                "--section", "preferences",
                "--key", "tone",
                "--summary", "warm collaboration",
                "--actor", "tester"
        );
        assertThat(record.exitCode()).isZero();
        assertThat(record.stdout()).contains("factId=");
        assertThat(record.stdout()).contains("section=preferences");

        CommandResult list = execute(runtime, "owner-profile", "list", "--being-id", beingId);
        assertThat(list.exitCode()).isZero();
        assertThat(list.stdout()).contains("key=tone");
        assertThat(list.stdout()).contains("summary=warm collaboration");
    }

    @Test
    void shouldManageManagedAgentSpecs() {
        CliRuntime runtime = CliRuntime.memory();

        String beingId = extractRequiredValue(
                execute(runtime, "being", "create", "--display-name", "Kai", "--actor", "tester").stdout(),
                "beingId"
        );

        CommandResult register = execute(
                runtime,
                "managed-agent",
                "register",
                "--being-id", beingId,
                "--role", "planner",
                "--status", "ACTIVE",
                "--actor", "tester"
        );
        assertThat(register.exitCode()).isZero();
        assertThat(register.stdout()).contains("managedAgentId=");
        assertThat(register.stdout()).contains("role=planner");

        CommandResult list = execute(runtime, "managed-agent", "list", "--being-id", beingId);
        assertThat(list.exitCode()).isZero();
        assertThat(list.stdout()).contains("status=ACTIVE");
    }

    @Test
    void shouldRenderCreateOutputAsJsonWhenRequested() {
        CliRuntime runtime = CliRuntime.memory();

        CommandResult create = execute(
                runtime,
                "--output", "json",
                "being", "create",
                "--display-name", "JsonNova",
                "--actor", "tester"
        );

        assertThat(create.exitCode()).isZero();
        assertThat(create.stdout()).contains("\"displayName\":\"JsonNova\"");
        assertThat(create.stdout()).contains("\"beingId\":");
    }

    @Test
    void shouldRenderListOutputAsJsonWhenRequested() {
        CliRuntime runtime = CliRuntime.memory();

        String beingId = extractRequiredValue(
                execute(runtime, "being", "create", "--display-name", "JsonLyra", "--actor", "tester").stdout(),
                "beingId"
        );
        assertThat(
                execute(
                        runtime,
                        "owner-profile",
                        "record",
                        "--being-id", beingId,
                        "--section", "preferences",
                        "--key", "style",
                        "--summary", "concise",
                        "--actor", "tester"
                ).exitCode()
        ).isZero();

        CommandResult list = execute(runtime, "--output", "json", "owner-profile", "list", "--being-id", beingId);
        assertThat(list.exitCode()).isZero();
        assertThat(list.stdout()).contains("\"section\":\"preferences\"");
        assertThat(list.stdout()).contains("\"summary\":\"concise\"");
        assertThat(list.stdout()).startsWith("[");
    }

    private static CommandResult execute(CliRuntime runtime, String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new DigitalBeingsCli(runtime));
        commandLine.setOut(new PrintWriter(stdout, true, StandardCharsets.UTF_8));
        commandLine.setErr(new PrintWriter(stderr, true, StandardCharsets.UTF_8));
        int exitCode = commandLine.execute(args);
        return new CommandResult(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8)
        );
    }

    private static String extractRequiredValue(String output, String key) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(key) + "=(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(output);
        if (!matcher.find()) {
            throw new AssertionError("Missing key " + key + " in output:\n" + output);
        }
        return matcher.group(1).trim();
    }

    private record CommandResult(int exitCode, String stdout, String stderr) {
    }
}
