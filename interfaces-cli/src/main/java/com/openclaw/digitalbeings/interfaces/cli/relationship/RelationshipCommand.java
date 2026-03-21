package com.openclaw.digitalbeings.interfaces.cli.relationship;

import com.openclaw.digitalbeings.application.relationship.CreateRelationshipEntityCommand;
import com.openclaw.digitalbeings.application.relationship.RelationshipEntityView;
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
        name = "relationship",
        mixinStandardHelpOptions = true,
        description = "Manage relationship entities.",
        subcommands = {
                RelationshipCommand.CreateCommand.class,
                RelationshipCommand.ListCommand.class
        }
)
public final class RelationshipCommand implements Runnable {

    @ParentCommand
    DigitalBeingsCli root;

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "create", mixinStandardHelpOptions = true, description = "Create a relationship entity.")
    public static final class CreateCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        RelationshipCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true)
        String beingId;

        @Option(names = "--kind", required = true)
        String kind;

        @Option(names = "--display-name", required = true)
        String displayName;

        @Option(names = "--actor", required = true)
        String actor;

        @Override
        public Integer call() {
            RelationshipEntityView view = parent.root.runtime().relationshipService().createRelationshipEntity(
                    new CreateRelationshipEntityCommand(beingId, kind, displayName, actor)
            );
            if (parent.root.outputFormat() == CliOutputFormat.JSON) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            print(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "list", mixinStandardHelpOptions = true, description = "List relationship entities for a being.")
    public static final class ListCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        RelationshipCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true)
        String beingId;

        @Override
        public Integer call() {
            List<RelationshipEntityView> views = parent.root.runtime().relationshipService().listRelationshipEntities(beingId);
            PrintWriter out = spec.commandLine().getOut();
            if (parent.root.outputFormat() == CliOutputFormat.JSON) {
                CliJsonSupport.printJson(out, views);
                return 0;
            }
            if (views.isEmpty()) {
                out.println("No relationship entities found.");
                return 0;
            }
            for (RelationshipEntityView view : views) {
                print(out, view);
                out.println();
            }
            return 0;
        }
    }

    private static void print(PrintWriter out, RelationshipEntityView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("relationshipEntityId=%s%n", view.relationshipEntityId());
        out.printf("kind=%s%n", view.kind());
        out.printf("displayName=%s%n", view.displayName());
        out.printf("recordedAt=%s%n", view.recordedAt());
    }
}
