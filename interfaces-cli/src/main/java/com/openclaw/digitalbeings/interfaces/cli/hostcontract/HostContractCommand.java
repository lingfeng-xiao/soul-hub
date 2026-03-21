package com.openclaw.digitalbeings.interfaces.cli.hostcontract;

import com.openclaw.digitalbeings.application.hostcontract.HostContractView;
import com.openclaw.digitalbeings.application.hostcontract.RegisterHostContractCommand;
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
        name = "host-contract",
        mixinStandardHelpOptions = true,
        description = "Manage host contracts.",
        subcommands = {
                HostContractCommand.CreateCommand.class,
                HostContractCommand.ListCommand.class
        }
)
public final class HostContractCommand implements Runnable {

    @ParentCommand
    DigitalBeingsCli root;

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "create", mixinStandardHelpOptions = true, description = "Create a host contract.")
    public static final class CreateCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        HostContractCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true)
        String beingId;

        @Option(names = "--host-type", required = true)
        String hostType;

        @Option(names = "--actor", required = true)
        String actor;

        @Override
        public Integer call() {
            HostContractView view = parent.root.runtime().hostContractService().registerHostContract(
                    new RegisterHostContractCommand(beingId, hostType, actor)
            );
            if (parent.root.outputFormat() == CliOutputFormat.JSON) {
                CliJsonSupport.printJson(spec.commandLine().getOut(), view);
                return 0;
            }
            print(spec.commandLine().getOut(), view);
            return 0;
        }
    }

    @Command(name = "list", mixinStandardHelpOptions = true, description = "List host contracts for a being.")
    public static final class ListCommand implements java.util.concurrent.Callable<Integer> {

        @ParentCommand
        HostContractCommand parent;

        @Spec
        CommandSpec spec;

        @Option(names = "--being-id", required = true)
        String beingId;

        @Override
        public Integer call() {
            List<HostContractView> views = parent.root.runtime().hostContractService().listHostContracts(beingId);
            PrintWriter out = spec.commandLine().getOut();
            if (parent.root.outputFormat() == CliOutputFormat.JSON) {
                CliJsonSupport.printJson(out, views);
                return 0;
            }
            if (views.isEmpty()) {
                out.println("No host contracts found.");
                return 0;
            }
            for (HostContractView view : views) {
                print(out, view);
                out.println();
            }
            return 0;
        }
    }

    private static void print(PrintWriter out, HostContractView view) {
        out.printf("beingId=%s%n", view.beingId());
        out.printf("contractId=%s%n", view.contractId());
        out.printf("hostType=%s%n", view.hostType());
        out.printf("status=%s%n", view.status());
        out.printf("registeredAt=%s%n", view.registeredAt());
    }
}
