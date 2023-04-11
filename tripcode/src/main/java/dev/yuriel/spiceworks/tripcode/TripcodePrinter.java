package dev.yuriel.spiceworks.tripcode;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class TripcodePrinter implements Runnable
{
    private final PrintStream ps;
    private final Python py;
    public String pattern;
    public boolean scanPattern = false;

    public TripcodePrinter(PrintStream ps, String pattern, boolean scanPattern, Python py) {
        this.ps = ps;
        this.pattern = pattern;
        this.scanPattern = scanPattern;
        this.py = py;
    }

    public TripcodePrinter(PrintStream ps, Python py) { this(ps, null, false, py); }

    @Override
    public void run()
    {
        if (this.scanPattern)
            this.printPattern();
        else
            this.printAll();
    }

    private void printAll()
    {
        List<String> passwords = new ArrayList<String>();
        do
        {
            int i = 0;
            passwords.clear();
            passwords.addAll(this.py.getNextPasswords(200));
        
            for (String password : passwords) {
                ps.printf("%s : %s%n", password, Tripcode.tripcode(password));
                if (i >= 5000) 
                {
                    System.out.printf("> [Worker %s] Printing tripcodes.%n", Thread.currentThread().getName());
                    ps.flush();
                    i = 0;
                }
                else
                    ++i;
            }
            ps.flush();  // case loop finishes before i reaches 5k
        }
        while (!passwords.isEmpty());
    }

    private void printPattern() throws IllegalArgumentException
    {
        if (this.pattern == null || this.pattern.isEmpty())
            throw new IllegalArgumentException("Cannot scan for a pattern when no pattern has been provided");
        
        List<String> passwords = new ArrayList<String>();
        do
        {
            passwords.clear();
            passwords.addAll(this.py.getNextPasswords());

            for (String password : passwords) {
                String code = Tripcode.tripcode(password);
                if (code.compareToIgnoreCase(this.pattern) == 0) {
                    ps.printf("%s -> %s%n", password, Tripcode.tripcode(password));
                    ps.flush();
                }
            }
        } while (!passwords.isEmpty());
    }
}
