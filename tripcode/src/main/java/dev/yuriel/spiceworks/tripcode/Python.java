package dev.yuriel.spiceworks.tripcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

public class Python
{
    private Process pythonInterpreter;
    private BufferedReader stdout;
    private PrintStream stdin;

    public Python() throws IOException
    {
        this.pythonInterpreter = new ProcessBuilder("python3", "-c", "while True: exec(input())").redirectErrorStream(true).start();
        this.stdout = new BufferedReader(new InputStreamReader(pythonInterpreter.getInputStream()));
        this.stdin = new PrintStream(pythonInterpreter.getOutputStream());
        if (!this.sanityCheck())
            System.err.println("Interpreter FAIL");
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

    public String sendCommand(String command)
    {
        try {
            this.stdin.println(command);
            this.stdin.flush();
            String result = this.stdout.readLine();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
