/*
 * Copyright (c) 2016-2017 Daniel Ennis (Aikar) - MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package co.aikar.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Verbatim copy of ACF 0.5.1's {@code co.aikar.commands.ProxyCommandMap}, with a single
 * change forced by the Bukkit API: Bukkit removed {@code SimpleCommandMap(Server)} (it now
 * only declares {@code SimpleCommandMap(Server, Map<String, Command>)}), so the shipped ACF
 * class would throw {@link NoSuchMethodError} the moment another plugin hijacks the command
 * map and ACF takes its "CommandMap has been hijacked!" branch.
 * <p>
 * The removed one-arg constructor was
 * <pre>{@code
 *   protected final Map<String, Command> knownCommands = new HashMap<>();
 *   public SimpleCommandMap(Server server) { this.server = server; setDefaultCommands(); }
 * }</pre>
 * i.e. it seeded {@code knownCommands} with a fresh, empty mutable map. {@code knownCommands}
 * is {@code protected final} and is now assigned straight from the constructor argument, so
 * there is no setter to use instead - passing a new {@link HashMap} is the exact equivalent
 * of the old behaviour (the two-arg constructor still calls {@code setDefaultCommands()}).
 * <p>
 * This file lives in ACF's own package so that it replaces the class inside the shaded jar;
 * it is relocated to {@code gg.jos.jossentials.acf} alongside the rest of ACF, and keeps the
 * original's package-private class/member visibility so {@code BukkitCommandManager} can
 * still construct it.
 */
class ProxyCommandMap extends SimpleCommandMap {
    private BukkitCommandManager manager;
    CommandMap proxied;

    ProxyCommandMap(BukkitCommandManager manager, CommandMap proxied) {
        super(Bukkit.getServer(), new HashMap<>());
        this.manager = manager;
        this.proxied = proxied;
    }

    @Override
    public void registerAll(String fallbackPrefix, List<Command> commands) {
        proxied.registerAll(fallbackPrefix, commands);
    }

    @Override
    public boolean register(String label, String fallbackPrefix, Command command) {
        if (isOurCommand(command)) {
            return super.register(label, fallbackPrefix, command);
        } else {
            return proxied.register(label, fallbackPrefix, command);
        }
    }

    boolean isOurCommand(String cmdLine) {
        String[] args = ACFPatterns.SPACE.split(cmdLine);
        return args.length != 0 && isOurCommand(this.knownCommands.get(args[0].toLowerCase(Locale.ENGLISH)));
    }

    boolean isOurCommand(Command command) {
        return command instanceof RootCommand && ((RootCommand) command).getManager() == this.manager;
    }

    @Override
    public boolean register(String fallbackPrefix, Command command) {
        if (isOurCommand(command)) {
            return super.register(fallbackPrefix, command);
        } else {
            return proxied.register(fallbackPrefix, command);
        }
    }

    @Override
    public boolean dispatch(CommandSender sender, String cmdLine) throws CommandException {
        if (isOurCommand(cmdLine)) {
            return super.dispatch(sender, cmdLine);
        } else {
            return proxied.dispatch(sender, cmdLine);
        }
    }

    @Override
    public void clearCommands() {
        super.clearCommands();
        proxied.clearCommands();
    }

    @Override
    public Command getCommand(String name) {
        if (isOurCommand(name)) {
            return super.getCommand(name);
        } else {
            return proxied.getCommand(name);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String cmdLine) throws IllegalArgumentException {
        if (isOurCommand(cmdLine)) {
            return super.tabComplete(sender, cmdLine);
        } else {
            return proxied.tabComplete(sender, cmdLine);
        }
    }
}
