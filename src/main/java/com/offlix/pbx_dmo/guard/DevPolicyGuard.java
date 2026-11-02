/**
 * DO NOT EDIT OR MODIFY THIS FILE
 */
package com.offlix.pbx_dmo.guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Profile("!prod")
public class DevPolicyGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevPolicyGuard.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path policyPath = Path.of("AGENTS.md");

        if (!Files.exists(policyPath) || Files.isSymbolicLink(policyPath)) {
            throw new IllegalStateException("❌ DEV ERROR: 'AGENTS.md' is missing or is a symlink! Do not delete it.");
        }
        Path realPath = policyPath.toRealPath();
        if (!realPath.getFileName().toString().equals("AGENTS.md")) {
            throw new IllegalStateException("❌ DEV ERROR: File must be named exactly 'AGENTS.md' (uppercase)!");
        }

        checkNotIgnored();
        checkGitIndexFlags();
        checkUncommittedChanges();

        log.info("integrity verified successfully.");
    }

    private void checkNotIgnored() {
        // Direct check on .gitignore
        Path gitIgnore = Path.of(".gitignore");
        if (Files.exists(gitIgnore)) {
            try {
                if (Files.readString(gitIgnore).toLowerCase().contains("agents.md")) {
                    throw new IllegalStateException("❌ DEV ERROR: 'AGENTS.md' cannot be added to .gitignore!");
                }
            } catch (Exception e) {
                if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            }
        }
        runGitCommand(
                new String[]{"git", "check-ignore", "-q", "AGENTS.md"},
                exitCode -> {
                    if (exitCode == 0) {
                        throw new IllegalStateException("❌ DEV ERROR: 'AGENTS.md' is being ignored by Git!");
                    }
                },
                null
        );
    }

    private void checkGitIndexFlags() {
        runGitCommand(
                new String[]{"git", "ls-files", "-v", "AGENTS.md"},
                null,
                output -> {
                    String trimmed = output.trim();
                    if (trimmed.startsWith("h") || trimmed.startsWith("S")) {
                        throw new IllegalStateException("❌ DEV ERROR: Git index flags modified on 'AGENTS.md'!");
                    }
                }
        );
    }

    private void checkUncommittedChanges() {
        runGitCommand(
                new String[]{"git", "status", "--porcelain", "AGENTS.md"},
                null,
                output -> {
                    String trimmed = output.trim();
                    if (trimmed.startsWith("M") || trimmed.contains(" M ") || trimmed.startsWith("D")) {
                        throw new IllegalStateException(
                                "❌ DEV ERROR: 'AGENTS.md' has been modified or deleted in Git!\n"
                                        + "Revert it to run the application: git checkout AGENTS.md"
                        );
                    }
                }
        );
    }

    private void runGitCommand(String[] command, ExitCodeConsumer onExit, OutputConsumer onOutput) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            if (onOutput != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        onOutput.accept(line);
                    }
                }
            }
            int exitCode = process.waitFor();
            if (onExit != null) {
                onExit.accept(exitCode);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception ignored) {

        }
    }

    @FunctionalInterface
    interface ExitCodeConsumer {
        void accept(int exitCode);
    }

    @FunctionalInterface
    interface OutputConsumer {
        void accept(String line);
    }
}
