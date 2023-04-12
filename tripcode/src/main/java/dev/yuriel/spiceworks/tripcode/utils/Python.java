package dev.yuriel.spiceworks.tripcode.utils;

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


/**
 * Python interpreter encapsulation class.<p>
 * 
 * This class holds a Python 3 interpreter instance in a separate process,
 * taking advantage of Python's generators and the {@code itertools.product()} function.<p>
 */
public class Python
{
    private Process pythonInterpreter;
    private BufferedReader stdout;
    private PrintStream stdin;
    private static final String charTable = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ !@$%*()[]{}\\-=_+;:/?,.|";
    private Gson gson;

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
        else {
            this.setupProducts();
            this.gson = new Gson();
        }
    }

    /**
     * Asserts that the underlying Python interpreter is receiving sent commands and that
     * this object can receive its input correctly.
     * 
     * @return A confirmation (or denial) that the underlying Python interpreter
     * works as intended.
     */
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

    /**
     * Performs the initial setup, importing {@code itertools.product()} and creating the
     * password character table.
     */
    private void setupProducts() {
        this.sendCommand("from itertools import product");
        this.sendCommand(String.format("char_table = '%s'", charTable));
    }

    /**
     * Creates a Python generator containing the cartesian product of the password
     * character table.
     * The passwords generated will have a length equal to {@code chars}.
     * 
     * @param chars The length of the passwords generated.
     */
    public void generatePasswords(int chars) {
        this.sendCommand(String.format("passwords = (''.join(i) for i in product(char_table, repeat=%d))", chars));
    }

    /**
     * Fetches the next passwords (up to 50) from the underlying Python generator.
     * 
     * @return A list containing up to 50 passwords, or an empty list if there are no
     * passwords left.
     */
    public synchronized List<String> getNextPasswords() { return this.getNextPasswords(50); }

    /**
     * Fetches the next passwords from the underlying Python generator.
     * 
     * @param howMany How many passwords to fetch.
     * @return A list containing up to {@code howMany} passwords, or an empty list if there are
     * no passwords left.
     */
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
