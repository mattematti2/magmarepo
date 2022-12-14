package org.magmafoundation.magma.installer;

import org.magmafoundation.magma.MagmaStart;
import org.magmafoundation.magma.common.MagmaConstants;
import org.magmafoundation.magma.common.utils.JarTool;
import org.magmafoundation.magma.common.utils.MD5;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Project: Magma
 *
 * @author Malcolm (M1lc0lm)
 * @date 03.07.2022 - 17:19
 *
 * Inspired by Shawiiz_z (https://github.com/Shawiizz)
 */
public abstract class AbstractMagmaInstaller {

    private PrintStream origin = System.out;
    public String forgeVer;
    public String mcpVer;
    public String mcVer;
    public String libPath = new File(JarTool.getJarDir(), MagmaConstants.INSTALLER_LIBRARIES_FOLDER).getAbsolutePath() + "/";

    public String forgeStart;
    public File universalJar;
    public File serverJar;

    public File lzma;
    public File installInfo;

    public String otherStart;
    public File extra;
    public File slim;
    public File srg;

    public String mcpStart;
    public File mcpZip;
    public File mcpTxt;

    public File minecraft_server;

    protected AbstractMagmaInstaller() {
        this.forgeVer = MagmaConstants.FORGE_VERSION_FULL.split("-")[1];
        this.mcpVer = MagmaConstants.FORGE_VERSION_FULL.split("-")[3];
        this.mcVer = MagmaConstants.FORGE_VERSION_FULL.split("-")[0];

        this.forgeStart = libPath + "net/minecraftforge/forge/" + mcVer + "-" + forgeVer + "/forge-" + mcVer + "-" + forgeVer;
        this.universalJar = new File(forgeStart + "-universal.jar");
        this.serverJar = new File(forgeStart + "-server.jar");

        this.lzma = new File(libPath + "org/magma/install/data/server.lzma");
        this.installInfo = new File(libPath + "org/magma/install/installInfo");

        this.otherStart = libPath + "net/minecraft/server/" + mcVer + "-" + mcpVer + "/server-" + mcVer + "-" + mcpVer;

        this.extra = new File(otherStart + "-extra.jar");
        this.slim = new File(otherStart + "-slim.jar");
        this.srg = new File(otherStart + "-srg.jar");

        this.mcpStart = libPath + "de/oceanlabs/mcp/mcp_config/" + mcVer + "-" + mcpVer + "/mcp_config-" + mcVer + "-" + mcpVer;
        this.mcpZip = new File(mcpStart + ".zip");
        this.mcpTxt = new File(mcpStart + "-mappings.txt");

        this.minecraft_server = new File(libPath + "minecraft_server." + mcVer + ".jar");
    }

    protected void launchService(String mainClass, List<String> args, List<URL> classPath) throws Exception {
        try {
            Class.forName(mainClass);
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + e.getMessage());
        }
        URLClassLoader loader = URLClassLoader.newInstance(classPath.toArray(new URL[0]));
        Class.forName(mainClass, true, loader).getDeclaredMethod("main", String[].class).invoke(null, (Object) args.toArray(new String[0]));
        loader.clearAssertionStatus();
        loader.close();
    }

    protected List<URL> stringToUrl(List<String> strs) throws Exception {
        List<URL> temp = new ArrayList<>();
        for (String t : strs) {
            temp.add(new File(t).toURI().toURL());
        }
        return temp;
    }

    /*
    THIS IS TO NOT SPAM CONSOLE WHEN IT WILL PRINT A LOT OF THINGS
     */
    protected void mute() throws Exception {
        File out = new File("logs/installer.log");
        if(!out.exists()) {
            out.getParentFile().mkdirs();
            out.createNewFile();
        }
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(out))));
    }

    protected void unmute() {
        System.setOut(origin);
    }

    protected void copyFileFromJar(File file, String pathInJar) throws Exception {
        InputStream is = MagmaStart.class.getClassLoader().getResourceAsStream(pathInJar);
        if(!file.exists() || !MD5.getMd5(file).equals(MD5.getMd5(is)) || file.length() <= 1) {
            file.getParentFile().mkdirs();
            file.createNewFile();
            if(is != null) Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            else {
                System.out.println("The file " + file.getName() + " was not found in the jar.");
                System.exit(0);
            }
        }
    }

    protected void deleteIfExists(File file) throws IOException {
        Files.deleteIfExists(file.toPath());
        File dir = file.getParentFile();
        if (dir.isDirectory() && dir.list().length == 0)
            Files.delete(dir.toPath());
    }

    protected void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for(File f : files) {
                if (f.isDirectory())
                    deleteFolder(f);
                else
                    f.delete();
            }
        }
        folder.delete();
    }

    protected boolean isCorrupted(File f) {
        try {
            JarFile j = new JarFile(f);
            j.close();
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    protected void restartServer(List<String> arguments) throws Exception {
        //clearConsole(); //Does not work atm

        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(AbstractMagmaInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        /* is it a jar file? */
        if(!currentJar.getName().endsWith(".jar"))
            return;

        /* Build command: java -jar application.jar */
        final ArrayList<String> command = new ArrayList<String>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());
        arguments.parallelStream().forEach(command::add);
        command.add("-postinstall");

        final ProcessBuilder builder = new ProcessBuilder(command);
        //System.out.println(Arrays.toString(builder.command().toArray()));
        builder.inheritIO().start().waitFor();
        Thread.sleep(2000);
        System.exit(0);
    }

    private void clearConsole()
    {
        try
        {
            final String os = System.getProperty("os.name");

            if (os.contains("Windows"))
            {
                Runtime.getRuntime().exec("cls");
            }
            else
            {
                Runtime.getRuntime().exec("clear");
            }
        }
        catch (final Exception e)
        {
            //  Handle any exceptions.
        }
    }

}