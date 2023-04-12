package dev.yuriel.spiceworks.tripcode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.Crypt;

import dev.yuriel.spiceworks.tripcode.utils.TripcodePrinter;
import dev.yuriel.spiceworks.tripcode.utils.Python;

/**
 * Main Tripcode generator class.
 * Depends on a Python 3 installation!
 */
public class Tripcode 
{
    private static final String saltTable = ".............................................../0123456789ABCDEFGABCDEFGHIJKLMNOPQRSTUVWXYZabcdefabcdefghijklmnopqrstuvwxyz.....................................................................................................................................";
    private static final String suffix = "H.";
    private PrintStream ps;
    private static final short cores = 6;
    private Python py;

    public Tripcode()
    {
        try {
            this.py = new Python();
        }
        catch (IOException e) {
            e.printStackTrace();
            this.py = null;
        }
    }

    public static void main(String[] args)
    {
        Tripcode tc = new Tripcode();
        String pattern = (args.length > 0)? args[0] : null;
        tc.scanTripcodes(pattern);
    }

    /**
     * Hashes a given password into a 10-character tripcode.
     * Passwords may not be longer than 8 characters, and will be truncated if needed.
     * 
     * @param password The password to generate a tripcode from.
     * @return The tripcode generated from the given password, or null if the password
     * is null or an empty string.
     */
    public static String tripcode(String password)
    {
        if (password == null || password.isEmpty())
            return null;
        if (password.length() > 8)
            password = password.substring(0, 8);
        
        String salt = "";
        for (int i = 1; i < 3; ++i) {
            int j = (int) password.concat(Tripcode.suffix).charAt(i);
            salt = new StringBuilder(salt).append(Tripcode.saltTable.charAt(j % 256)).toString();
        }
        return Crypt.crypt(password, salt).substring(3);
    }

    /**
     * Generates tripcodes.<p>
     * 
     * A new file named "codes.txt" will be created in the current working directory,
     * where ALL tripcodes generated will be written to.
     */
    public void scanTripcodes() { this.scanTripcodes(null); }

    /**
     * Scans for tripcodes.<p>
     * 
     * If a pattern is given, any tripcodes containing said pattern (case insensitive)
     * will be outputted to the standard output.<p>
     * 
     * Otherwise, a new file named "codes.txt" will be created in the current working
     * directory, where ALL tripcodes generated will be written to.<p>
     * 
     * <b>Note:</b> This method makes use of threads to speed up execution time.
     * The amount of threads used is equal to the {@code cores} attribute.
     * 
     * @param pattern The pattern to scan for
     */
    public void scanTripcodes(String pattern)
    {
        boolean hasPattern = (pattern != null && !pattern.isEmpty());
        try
        {
            if (!hasPattern)
                this.ps = new PrintStream("codes.txt");
            else
                this.ps = System.out;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        for (int i = 1; i <= 8; ++i)
        {
            ExecutorService ex = Executors.newFixedThreadPool(Tripcode.cores);
            this.py.generatePasswords(i);
            String msg;
            if (hasPattern)
                msg = String.format("> Scanning for pattern '%s' (case insensitive) using %d-character passwords. ", pattern, i);
            else
                msg = String.format("> Generating tripcodes with %d-character passwords. ", i);
            System.out.println(msg.concat(String.format("Using %d thread(s).", Tripcode.cores)));

            for (int j = 0; j < Tripcode.cores; ++j) {
                Runnable worker = new TripcodePrinter(this.ps, pattern, hasPattern, this.py);
                ex.submit(worker);
            }

            ex.shutdown();
            try {
                ex.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * <i><b>Deprecated:</b> Due to the thread pool being recreated every iteration, awaiting shutdown yields the same results
     * without the need to use a CompletionService.</i><p>
     * 
     * Awaits the execution of a certain amount of runnables in the given CompletionService instance.
     * 
     * @param ex The CompletionService to await Runnables from;
     * @param threadAmount The amount of Runnables to await.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static void waitForAllRunnables(CompletionService<Void> ex, int threadAmount)
    {
        for (int j = 0; j < threadAmount; ++j)
        {
            try {
                Future<Void> future = ex.take();
                Void v = future.get();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
