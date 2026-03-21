package com.openclaw.digitalbeings.interfaces.cli;

import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.IdentityFacetView;
import com.openclaw.digitalbeings.application.lease.AcquireAuthorityLeaseCommand;
import com.openclaw.digitalbeings.application.lease.LeaseView;
import com.openclaw.digitalbeings.application.lease.RegisterRuntimeSessionCommand;
import com.openclaw.digitalbeings.application.lease.ReleaseAuthorityLeaseCommand;
import com.openclaw.digitalbeings.application.lease.RuntimeSessionView;
import com.openclaw.digitalbeings.application.review.CanonicalProjectionView;
import com.openclaw.digitalbeings.application.review.DecideReviewCommand;
import com.openclaw.digitalbeings.application.review.DraftReviewCommand;
import com.openclaw.digitalbeings.application.review.RebuildCanonicalProjectionCommand;
import com.openclaw.digitalbeings.application.review.ReviewDecision;
import com.openclaw.digitalbeings.application.review.ReviewItemView;
import com.openclaw.digitalbeings.application.review.SubmitReviewCommand;
import com.openclaw.digitalbeings.application.governance.ManagedAgentSpecView;
import com.openclaw.digitalbeings.application.governance.OwnerProfileFactView;
import com.openclaw.digitalbeings.application.governance.RecordOwnerProfileFactCommand;
import com.openclaw.digitalbeings.application.governance.RegisterManagedAgentSpecCommand;
import com.openclaw.digitalbeings.interfaces.cli.hostcontract.HostContractCommand;
import com.openclaw.digitalbeings.interfaces.cli.relationship.RelationshipCommand;
import com.openclaw.digitalbeings.interfaces.cli.snapshot.SnapshotCommand;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(
        name = "digital-beings",
        mixinStandardHelpOptions = true,
        version = "digital-beings-java CLI 0.1.0",
        description = "Command-line access to the Digital Beings application services.",
        subcommands = {
                DigitalBeingsCli.BeingCommand.class,
                DigitalBeingsCli.LeaseCommand.class,
                DigitalBeingsCli.ReviewCommand.class,
                DigitalBeingsCli.ProjectionCommand.class,
                RelationshipCommand.class,
                HostContractCommand.class,
                SnapshotCommand.class,
                DigitalBeingsCli.OwnerProfileCommand.class,
                DigitalBeingsCli.ManagedAgentCommand.class
        }
)
public final class DigitalBeingsCli implements Runnable {

    private final CliRuntime runtime;

    @Spec
    CommandSpec spec;

    @Option(
            names = "--output",
            defaultValue = "table",
            converter = CliOutputFormatConverter.class,
            description = "Output format: table or json."
    )
    CliOutputFormat outputFormat;

    public DigitalBeingsCli() {
        this(CliRuntime.memory());
    }

    public DigitalBeingsCli(CliRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    public CliRuntime runtime() {
        return runtime;
    }

    public CliOutputFormat outputFormat() {
        return outputFormat;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DigitalBeingsCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    private static void printBeing(PrintWriter out, BeingView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("displayName=%s%n", view.displayName());
        out.printf("revision=%d%n", view.revision());
        out.printf("createdAt=%s%n", view.createdAt());
        out.printf("identityFacetCount=%d%n", view.identityFacetCount());
        out.printf("relationshipCount=%d%n", view.relationshipCount());
        out.printf("runtimeSessionCount=%d%n", view.runtimeSessionCount());
        out.printf("activeLeaseCount=%d%n", view.activeLeaseCount());
        out.printf("reviewItemCount=%d%n", view.reviewItemCount());
        out.printf("acceptedReviewItemCount=%d%n", view.acceptedReviewItemCount());
        out.printf("canonicalProjectionVersion=%s%n", view.canonicalProjectionVersion());
    }

    private static void printLease(PrintWriter out, LeaseView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("leaseId=%s%n", view.leaseId());
        out.printf("sessionId=%s%n", view.sessionId());
        out.printf("status=%s%n", view.status());
        out.printf("requestedAt=%s%n", view.requestedAt());
        out.printf("grantedAt=%s%n", view.grantedAt());
        out.printf("releasedAt=%s%n", view.releasedAt());
        out.printf("lastActor=%s%n", view.lastActor());
    }

    private static void printSession(PrintWriter out, RuntimeSessionView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("sessionId=%s%n", view.sessionId());
        out.printf("hostType=%s%n", view.hostType());
        out.printf("startedAt=%s%n", view.startedAt());
        out.printf("endedAt=%s%n", view.endedAt());
    }

    private static void printReview(PrintWriter out, ReviewItemView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("reviewItemId=%s%n", view.reviewItemId());
        out.printf("lane=%s%n", view.lane());
        out.printf("kind=%s%n", view.kind());
        out.printf("proposal=%s%n", view.proposal());
        out.printf("status=%s%n", view.status());
        out.printf("createdAt=%s%n", view.createdAt());
        out.printf("updatedAt=%s%n", view.updatedAt());
        out.printf("lastActor=%s%n", view.lastActor());
    }

    private static void printProjection(PrintWriter out, CanonicalProjectionView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("projectionId=%s%n", view.projectionId());
        out.printf("version=%d%n", view.version());
        out.printf("generatedAt=%s%n", view.generatedAt());
        out.printf("acceptedReviewItemIds=%s%n", view.acceptedReviewItemIds());
        out.printf("contentSummary=%s%n", view.contentSummary());
    }

    private static void printOwnerProfileFact(PrintWriter out, OwnerProfileFactView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("factId=%s%n", view.factId());
        out.printf("section=%s%n", view.section());
        out.printf("key=%s%n", view.key());
        out.printf("summary=%s%n", view.summary());
        out.printf("acceptedAt=%s%n", view.acceptedAt());
    }

    private static void printManagedAgentSpec(PrintWriter out, ManagedAgentSpecView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("managedAgentId=%s%n", view.managedAgentId());
        out.printf("role=%s%n", view.role());
        out.printf("status=%s%n", view.status());
        out.printf("createdAt=%s%n", view.createdAt());
    }

    private static boolean isJson(CliOutputFormat outputFormat) {
        return outputFormat == CliOutputFormat.JSON;
    }

    @Command(
            name = "being",
            mixinStandardHelpOptions = true,
            description = "Create and inspect beings.",
            subcommands = {
                    BeingCreateCommand.class,
                    BeingGetCommand.class,
                    BeingListCommand.class,
                    IdentityFacetAddCommand.class
            }
    )
    public static final class BeingCommand implements Runnable {

        @ParentCommand
        DigitalBeingsCli root;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @Command(name = "create", mixinStandardHelpOptions = true, description = "Create a being.")
    public static final class BeingCreateCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        BeingCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--display-name", required = true, description = "Display name for the being.")
        String displayName;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            BeingView view = parent.root.runtime.beingService().createBeing(new com.openclaw.digitalbeings.application.being.CreateBeingCommand(displayName, actor));
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printBeing(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "get", mixinStandardHelpOptions = true, description = "Fetch a being by id.")
    public static final class BeingGetCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        BeingCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Override
        public Integer call() {
            BeingView view = parent.root.runtime.beingService().getBeing(beingId);
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printBeing(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "list", mixinStandardHelpOptions = true, description = "List all beings.")
    public static final class BeingListCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        BeingCommand parent;

        @Spec
        CommandSpec spec;

        @Override
        public Integer call() {
            List<BeingView> beings = parent.root.runtime.beingService().listBeings();
            PrintWriter out = spec.commandLine().getOut();
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(out, beings);
                return 0;
            }
            if (beings.isEmpty()) {
                out.println("No beings found.");
                return 0;
            }
            for (BeingView view : beings) {
                printBeing(out, view);
                out.println();
            }
            return 0;
        }
    }

    @Command(name = "add-identity-facet", mixinStandardHelpOptions = true, description = "Add an identity facet to a being.")
    public static final class IdentityFacetAddCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        BeingCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--kind", required = true, description = "Identity facet kind.")
        String kind;

        @Option(names = "--summary", required = true, description = "Identity facet summary.")
        String summary;

        @Option(names = "--actor", defaultValue = "CLI", description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            IdentityFacetView view = parent.root.runtime.beingService().addIdentityFacet(beingId, kind, summary, actor, java.time.Instant.now());
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            spec.commandLine().getOut().printf("facetId=%s%n", view.facetId());
            spec.commandLine().getOut().printf("kind=%s%n", view.kind());
            spec.commandLine().getOut().printf("summary=%s%n", view.summary());
            spec.commandLine().getOut().printf("recordedAt=%s%n", view.recordedAt());
            return 0;
        }
    }

    @Command(
            name = "lease",
            mixinStandardHelpOptions = true,
            description = "Manage runtime sessions and authority leases.",
            subcommands = {
                    RegisterSessionCommand.class,
                    AcquireLeaseCommand.class,
                    ReleaseLeaseCommand.class,
                    CloseSessionCommand.class,
                    ExpireLeaseCommand.class,
                    RevokeLeaseCommand.class
            }
    )
    public static final class LeaseCommand implements Runnable {

        @ParentCommand
        DigitalBeingsCli root;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @Command(name = "register-session", mixinStandardHelpOptions = true, description = "Register a runtime session.")
    public static final class RegisterSessionCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        LeaseCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--host-type", required = true, description = "Host type for the session.")
        String hostType;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            RuntimeSessionView view = parent.root.runtime.leaseService().registerRuntimeSession(new RegisterRuntimeSessionCommand(beingId, hostType, actor));
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printSession(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "acquire", mixinStandardHelpOptions = true, description = "Acquire an authority lease.")
    public static final class AcquireLeaseCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        LeaseCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--session-id", required = true, description = "Runtime session identifier.")
        String sessionId;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            LeaseView view = parent.root.runtime.leaseService().acquireAuthorityLease(new AcquireAuthorityLeaseCommand(beingId, sessionId, actor));
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printLease(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "release", mixinStandardHelpOptions = true, description = "Release an authority lease.")
    public static final class ReleaseLeaseCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        LeaseCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--lease-id", required = true, description = "Authority lease identifier.")
        String leaseId;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            LeaseView view = parent.root.runtime.leaseService().releaseAuthorityLease(new ReleaseAuthorityLeaseCommand(beingId, leaseId, actor));
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printLease(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "close-session", mixinStandardHelpOptions = true, description = "Close a runtime session.")
    public static final class CloseSessionCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        LeaseCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--session-id", required = true, description = "Runtime session identifier.")
        String sessionId;

        @Option(names = "--actor", defaultValue = "CLI", description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            RuntimeSessionView view = parent.root.runtime.leaseService().closeSession(beingId, sessionId, actor);
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printSession(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "expire-lease", mixinStandardHelpOptions = true, description = "Expire an authority lease.")
    public static final class ExpireLeaseCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        LeaseCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--lease-id", required = true, description = "Authority lease identifier.")
        String leaseId;

        @Option(names = "--actor", defaultValue = "CLI", description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            LeaseView view = parent.root.runtime.leaseService().expireLease(beingId, leaseId, actor);
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printLease(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "revoke-lease", mixinStandardHelpOptions = true, description = "Revoke an authority lease.")
    public static final class RevokeLeaseCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        LeaseCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--lease-id", required = true, description = "Authority lease identifier.")
        String leaseId;

        @Option(names = "--actor", defaultValue = "CLI", description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            LeaseView view = parent.root.runtime.leaseService().revokeLease(beingId, leaseId, actor);
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printLease(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(
            name = "review",
            mixinStandardHelpOptions = true,
            description = "Manage review items.",
            subcommands = {
                    DraftCommand.class,
                    SubmitCommand.class,
                    DecideCommand.class
            }
    )
    public static final class ReviewCommand implements Runnable {

        @ParentCommand
        DigitalBeingsCli root;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @Command(name = "draft", mixinStandardHelpOptions = true, description = "Create a draft review item.")
    public static final class DraftCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        ReviewCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--lane", required = true, description = "Review lane.")
        String lane;

        @Option(names = "--kind", required = true, description = "Review kind.")
        String kind;

        @Option(names = "--proposal", required = true, description = "Review proposal.")
        String proposal;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            ReviewItemView view = parent.root.runtime.reviewService().draftReview(new DraftReviewCommand(beingId, lane, kind, proposal, actor));
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printReview(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "submit", mixinStandardHelpOptions = true, description = "Submit a draft review item.")
    public static final class SubmitCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        ReviewCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--review-item-id", required = true, description = "Review item identifier.")
        String reviewItemId;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            ReviewItemView view = parent.root.runtime.reviewService().submitReview(new SubmitReviewCommand(beingId, reviewItemId, actor));
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printReview(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "decide", mixinStandardHelpOptions = true, description = "Decide a submitted review item.")
    public static final class DecideCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        ReviewCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--review-item-id", required = true, description = "Review item identifier.")
        String reviewItemId;

        @Option(names = "--decision", required = true, description = "Decision to apply: ACCEPTED, REJECTED, or DEFERRED.")
        ReviewDecision decision;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            ReviewItemView view = parent.root.runtime.reviewService().decideReview(new DecideReviewCommand(beingId, reviewItemId, decision, actor));
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printReview(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(
            name = "projection",
            mixinStandardHelpOptions = true,
            description = "Read or rebuild the canonical projection.",
            subcommands = {
                    ProjectionReadCommand.class,
                    ProjectionRebuildCommand.class
            }
    )
    public static final class ProjectionCommand implements Runnable {

        @ParentCommand
        DigitalBeingsCli root;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @Command(name = "read", mixinStandardHelpOptions = true, description = "Read the canonical projection for a being.")
    public static final class ProjectionReadCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        ProjectionCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Override
        public Integer call() {
            CanonicalProjectionView view = parent.root.runtime.reviewService().getCanonicalProjection(beingId);
            PrintWriter out = spec.commandLine().getOut();
            if (view == null) {
                if (isJson(parent.root.outputFormat())) {
                    LinkedHashMap<String, Object> data = new LinkedHashMap<>();
                    data.put("beingId", beingId);
                    data.put("canonicalProjection", "absent");
                    CliJsonSupport.printJson(out, data);
                    return 0;
                }
                out.printf("beingId=%s%n", beingId);
                out.println("canonicalProjection=absent");
                return 0;
            }
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(out, view);
                return 0;
            }
            printProjection(out, view);
            return 0;
        }
    }

    @Command(name = "rebuild", mixinStandardHelpOptions = true, description = "Rebuild the canonical projection for a being.")
    public static final class ProjectionRebuildCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        ProjectionCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            CanonicalProjectionView view = parent.root.runtime.reviewService().rebuildCanonicalProjection(new RebuildCanonicalProjectionCommand(beingId, actor));
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printProjection(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(
            name = "owner-profile",
            mixinStandardHelpOptions = true,
            description = "Record and inspect owner profile facts.",
            subcommands = {
                    OwnerProfileRecordCommand.class,
                    OwnerProfileListCommand.class
            }
    )
    public static final class OwnerProfileCommand implements Runnable {

        @ParentCommand
        DigitalBeingsCli root;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @Command(name = "record", mixinStandardHelpOptions = true, description = "Record an owner profile fact.")
    public static final class OwnerProfileRecordCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        OwnerProfileCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--section", required = true, description = "Profile section.")
        String section;

        @Option(names = "--key", required = true, description = "Fact key.")
        String key;

        @Option(names = "--summary", required = true, description = "Fact summary.")
        String summary;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            OwnerProfileFactView view = parent.root.runtime.governanceService().recordOwnerProfileFact(
                    new RecordOwnerProfileFactCommand(beingId, section, key, summary, actor)
            );
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printOwnerProfileFact(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "list", mixinStandardHelpOptions = true, description = "List owner profile facts for a being.")
    public static final class OwnerProfileListCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        OwnerProfileCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Override
        public Integer call() {
            List<OwnerProfileFactView> facts = parent.root.runtime.governanceService().listOwnerProfileFacts(beingId);
            PrintWriter out = spec.commandLine().getOut();
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(out, facts);
                return 0;
            }
            if (facts.isEmpty()) {
                out.println("No owner profile facts found.");
                return 0;
            }
            for (OwnerProfileFactView view : facts) {
                printOwnerProfileFact(out, view);
                out.println();
            }
            return 0;
        }
    }

    @Command(
            name = "managed-agent",
            mixinStandardHelpOptions = true,
            description = "Register and inspect managed agent specs.",
            subcommands = {
                    ManagedAgentRegisterCommand.class,
                    ManagedAgentListCommand.class
            }
    )
    public static final class ManagedAgentCommand implements Runnable {

        @ParentCommand
        DigitalBeingsCli root;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @Command(name = "register", mixinStandardHelpOptions = true, description = "Register a managed agent spec.")
    public static final class ManagedAgentRegisterCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        ManagedAgentCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Option(names = "--role", required = true, description = "Agent role.")
        String role;

        @Option(names = "--status", required = true, description = "Agent status.")
        String status;

        @Option(names = "--actor", required = true, description = "Actor performing the action.")
        String actor;

        @Override
        public Integer call() {
            ManagedAgentSpecView view = parent.root.runtime.governanceService().registerManagedAgentSpec(
                    new RegisterManagedAgentSpecCommand(beingId, role, status, actor)
            );
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            printManagedAgentSpec(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "list", mixinStandardHelpOptions = true, description = "List managed agent specs for a being.")
    public static final class ManagedAgentListCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        ManagedAgentCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true, description = "Being identifier.")
        String beingId;

        @Override
        public Integer call() {
            List<ManagedAgentSpecView> specs = parent.root.runtime.governanceService().listManagedAgentSpecs(beingId);
            PrintWriter out = spec.commandLine().getOut();
            if (isJson(parent.root.outputFormat())) {
                CliJsonSupport.printJson(out, specs);
                return 0;
            }
            if (specs.isEmpty()) {
                out.println("No managed agent specs found.");
                return 0;
            }
            for (ManagedAgentSpecView view : specs) {
                printManagedAgentSpec(out, view);
                out.println();
            }
            return 0;
        }
    }
}
