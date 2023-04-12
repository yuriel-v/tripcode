package dev.yuriel.spiceworks.tripcode.utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dev.yuriel.spiceworks.tripcode.Tripcode;


/**
 * Workhorse of the generator program.<p>
 * 
 * This Runnable, when given a properly configured {@link dev.yuriel.spiceworks.tripcode.utils.Python Python} object,
 * will generate all tripcodes it manages to fetch from it.<p>
 * 
 * When given a pattern, will print only tripcodes matching that pattern.
 * Otherwise, it prints all tripcodes generated indiscriminately.
 */
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

    /**
     * Prints all tripcodes generated to its given {@link java.io.PrintStream PrintStream} instance.
     */
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

    /**
     * Prints all tripcodes generated to its given {@link java.io.PrintStream PrintStream} instance,
     * as long as they match its given pattern.
     * 
     * @throws IllegalArgumentException When the given pattern is null or empty. Use {@link #printAll printAll()} instead.
     * @see {@link #printAll printAll()} - The indiscriminate version of this method.
     */
    private void printPattern() throws IllegalArgumentException
    {
        if (this.pattern == null || this.pattern.isEmpty())
            throw new IllegalArgumentException("Cannot scan for a pattern when no pattern has been provided");
        
        List<String> passwords = new ArrayList<String>();
        Pattern regex = Pattern.compile(Pattern.quote(this.pattern), Pattern.CASE_INSENSITIVE);
        do
        {
            passwords.clear();
            passwords.addAll(this.py.getNextPasswords());

            for (String password : passwords) {
                String code = Tripcode.tripcode(password);
                boolean found = regex.matcher(code).find();
                if (found) {
                    ps.printf("%s -> %s%n", password, Tripcode.tripcode(password));
                    ps.flush();
                }
            }
        } while (!passwords.isEmpty());
    }
}
