package org.magmafoundation.magma;/*
 * Magma Server
 * Copyright (C) 2019-2022.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.magmafoundation.magma.betterui.BetterUI;
import org.magmafoundation.magma.installer.MagmaInstaller;
import org.magmafoundation.magma.updater.MagmaUpdater;
import org.magmafoundation.magma.utils.BootstrapLauncher;
import org.magmafoundation.magma.common.utils.JarTool;
import org.magmafoundation.magma.utils.ServerInitHelper;
import org.magmafoundation.magma.common.utils.SystemType;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.magmafoundation.magma.common.MagmaConstants.*;


/**
 * Project: Magma
 *
 * @author Malcolm (M1lc0lm) / Hexeption
 * @date 03.07.2022 - 17:19
 */
public class MagmaStart {

    public static void main(String[] args) throws Exception {
        if (Arrays.stream(args).anyMatch(s -> s.equalsIgnoreCase("-noui"))) {
            BetterUI.setEnabled(false);
            args = remove(args, "-noui");
        }
        if (Arrays.stream(args).anyMatch(s -> s.equalsIgnoreCase("-nologo"))) {
            BetterUI.setEnableBigLogo(false);
            args = remove(args, "-nologo");
        }
        Path eula = Paths.get("eula.txt");
        if (Arrays.stream(args).anyMatch(s -> s.equalsIgnoreCase("-accepteula"))) {
            BetterUI.forceAcceptEULA(eula);
            args = remove(args, "-accepteula");
        }
        boolean enableUpdate = true;
        if (Arrays.stream(args).anyMatch(s -> s.equalsIgnoreCase("-dau"))) {
            enableUpdate = false;
            args = remove(args, "-dau");
        }
        if (Arrays.stream(args).anyMatch(s -> s.equalsIgnoreCase("-nojline"))) {
            args = remove(args, "-nojline"); //For some reason when passing -nojline to the console the whole thing crashes, remove this
        }

        BetterUI.printTitle(NAME, BRAND, System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")", VERSION, BUKKIT_VERSION, FORGE_VERSION);

        //Temporary warning for people using the new server jar
        System.err.println("WARNING: The new server jar is still under development and will be unstable! If you experience any issues, please report them to the developers.");
        System.err.println("WARNING: If the server crashes while installing, try removing the libraries folder and launching the server again.");
        //Temporary warning for people using the new server jar

        if(!BetterUI.checkEula(eula)) System.exit(0);

        List<String> launchArgs = JarTool.readFileLinesFromJar("data/" + (SystemType.getOS().equals(SystemType.OS.WINDOWS) ? "win" : "unix") + "_args.txt");
        List<String> forgeArgs = new ArrayList<>();
        launchArgs.stream().filter(s -> s.startsWith("--launchTarget") || s.startsWith("--fml.forgeVersion") || s.startsWith("--fml.mcVersion") || s.startsWith("--fml.forgeGroup") || s.startsWith("--fml.mcpVersion")).toList().forEach(arg -> {
            forgeArgs.add(arg.split(" ")[0]);
            forgeArgs.add(arg.split(" ")[1]);
        });

        new MagmaInstaller();

        ServerInitHelper.init(launchArgs);

        Path path = Paths.get("magma.yml");
        if (Files.exists(path)) {
            try (InputStream stream = Files.newInputStream(path)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(stream);
                Map<String, Object> forge = (Map<String, Object>) data.get("magma");
                MagmaUpdater updater = new MagmaUpdater();
                if (enableUpdate) {
                    System.out.println("Checking for updates...");
                    if (updater.versionChecker() && forge.get("auto-update").equals(true))
                        updater.downloadJar();
                }
            }
        }

        String[] invokeArgs = Stream.concat(forgeArgs.stream(), Stream.of(args)).toArray(String[]::new);
        BootstrapLauncher.startServer(invokeArgs);
    }

    private static String[] remove(String[] array, String element) {
        if (array.length > 0) {
            int index = -1;
            for (int i = 0; i < array.length; i++) {
                if (array[i].equalsIgnoreCase(element)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                String[] copy = (String[]) Array.newInstance(array.getClass()
                        .getComponentType(), array.length - 1);
                if (copy.length > 0) {
                    System.arraycopy(array, 0, copy, 0, index);
                    System.arraycopy(array, index + 1, copy, index, copy.length - index);
                }
                return copy;
            }
        }
        return array;
    }
}
