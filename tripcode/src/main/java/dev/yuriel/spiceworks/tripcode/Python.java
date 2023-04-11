package dev.yuriel.spiceworks.tripcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Python
{
    private Process pythonInterpreter;
    private BufferedReader stdout;
    private PrintStream stdin;
    private static final String charTable = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ !@$%*()[]{}\\-=_+;:/?,.|";
    private final Gson gson = new Gson();

    public Python() throws IOException
    {
        boolean linux = System.getProperty("os.name").compareToIgnoreCase("linux") == 0;
        this.pythonInterpreter = new ProcessBuilder(
            "python" + (linux? "3" : ""),
            "-c",
            "while True: exec(input())")
            .redirectErrorStream(true)
            .start();
        this.stdout = new BufferedReader(new InputStreamReader(pythonInterpreter.getInputStream()));
        this.stdin = new PrintStream(pythonInterpreter.getOutputStream());
        if (!this.sanityCheck())
            System.err.println("Interpreter FAIL");
        else
            this.setupProducts();
    }

    private boolean sanityCheck() {
        String result = this.sendCommand("print(2 + 2)");
        return result.compareTo("4") == 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable
    {
        this.stdin.write("exit(0)%n".getBytes());
        this.pythonInterpreter.destroy();
        this.pythonInterpreter.waitFor(10, TimeUnit.SECONDS);
        super.finalize();
    }

    /**
     * Sends a Python command to be processed by the underlying interpreter.
     * 
     * @param command The Python command to be executed.
     * @return A string containing the command's return, or null if the command was not a print statement.
     */
    public String sendCommand(String command)
    {
        try
        {
            this.stdin.println(command);
            this.stdin.flush();
            String result = command.startsWith("print(")? this.stdout.readLine() : null;
            return result;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupProducts() {
        this.sendCommand("from itertools import product");
        this.sendCommand(String.format("char_table = '%s'", charTable));
    }

    public void generatePasswords(int chars) {
        this.sendCommand(String.format("passwords = (''.join(i) for i in product(char_table, repeat=%d))", chars));
    }

    public synchronized List<String> getNextPasswords() { return this.getNextPasswords(50); }
    public synchronized List<String> getNextPasswords(int howMany)
    {
        String passwords = this.sendCommand(String.format("print([next(passwords, None) for _ in range(%d)])", howMany));
        if (passwords.startsWith("[None,"))
            return List.of();
        else
        {
            List<String> result = this.gson.fromJson(passwords, new TypeToken<ArrayList<String>>(){}.getType());
            return result.stream().filter(p -> p.compareTo("None") != 0).collect(Collectors.toList());
        }
    }
}
