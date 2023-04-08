package dev.yuriel.spiceworks.tripcode;

import java.io.PrintStream;
import java.util.List;

public class TripcodePrinter implements Runnable
{
    private final PrintStream ps;
    private final List<String> passwords;
    public String pattern;
    public boolean scanPattern = false;

    public TripcodePrinter(PrintStream ps, List<String> passwords, String pattern, boolean scanPattern) {
        this.ps = ps;
        this.passwords = passwords;
        this.pattern = pattern;
        this.scanPattern = scanPattern;
    }

    public TripcodePrinter(PrintStream ps, List<String> passwords) { this(ps, passwords, null, false); }

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
        int i = 0;
        for (String password : this.passwords) {
            ps.printf("%s : %s%n", password, Tripcode.tripcode(password));
            if (i >= 5000) {
                ps.flush();
                i = 0;
            }
            else
                ++i;
        }
        ps.flush();
    }

    private void printPattern() throws IllegalArgumentException
    {
        if (this.pattern == null || this.pattern.isEmpty())
            throw new IllegalArgumentException("Cannot scan for a pattern when no pattern has been provided");
        
        int i = 0;
        for (String password : this.passwords) {
            String code = Tripcode.tripcode(password);
            if (code.compareToIgnoreCase(this.pattern) == 0)
            {
                ps.printf("%s : %s%n", password, Tripcode.tripcode(password));
                if (i >= 5000) {
                    ps.flush();
                    i = 0;
                }
                else
                    ++i;
            }
        }
        ps.flush();
    }
}
