package com.openclaw.digitalbeings.interfaces.cli.snapshot;

import com.openclaw.digitalbeings.application.snapshot.CreateSnapshotCommand;
import com.openclaw.digitalbeings.application.snapshot.SnapshotView;
import com.openclaw.digitalbeings.domain.core.SnapshotType;
import com.openclaw.digitalbeings.interfaces.cli.CliJsonSupport;
import com.openclaw.digitalbeings.interfaces.cli.CliOutputFormat;
import com.openclaw.digitalbeings.interfaces.cli.DigitalBeingsCli;
import java.io.PrintWriter;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(
        name = "snapshot",
        mixinStandardHelpOptions = true,
        description = "Manage continuity snapshots.",
        subcommands = {
                SnapshotCommand.CreateCommand.class,
                SnapshotCommand.ListCommand.class
        }
)
public final class SnapshotCommand implements Runnable {

    @ParentCommand
    DigitalBeingsCli root;

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "create", mixinStandardHelpOptions = true, description = "Create a continuity snapshot.")
    public static final class CreateCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        SnapshotCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true)
        String beingId;

        @Option(names = "--type", required = true)
        SnapshotType type;

        @Option(names = "--summary", required = true)
        String summary;

        @Option(names = "--actor", required = true)
        String actor;

        @Override
        public Integer call() {
            SnapshotView view = parent.root.runtime().snapshotService().createSnapshot(
                    new CreateSnapshotCommand(beingId, type, summary, actor)
            );
            if (parent.root.outputFormat() == CliOutputFormat.JSON) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            print(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "list", mixinStandardHelpOptions = true, description = "List continuity snapshots for a being.")
    public static final class ListCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        SnapshotCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true)
        String beingId;

        @Override
        public Integer call() {
            List<SnapshotView> views = parent.root.runtime().snapshotService().listSnapshots(beingId);
            PrintWriter out = spec.commandLine().getOut();
            if (parent.root.outputFormat() == CliOutputFormat.JSON) {
                CliJsonSupport.printJson(out, views);
                return 0;
            }
            if (views.isEmpty()) {
                out.println("No continuity snapshots found.");
                return 0;
            }
            for (SnapshotView view : views) {
                print(out, view);
                out.println();
            }
            return 0;
        }
    }

    private static void print(PrintWriter out, SnapshotView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("snapshotId=%s%n", view.snapshotId());
        out.printf("type=%s%n", view.type());
        out.printf("summary=%s%n", view.summary());
        out.printf("createdAt=%s%n", view.createdAt());
    }
}
