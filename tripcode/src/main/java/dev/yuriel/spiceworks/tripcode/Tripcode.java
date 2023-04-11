package dev.yuriel.spiceworks.tripcode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.Crypt;

/**
 * Hello world!
 *
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
        //String code = "a";
        //System.out.printf("Password: %s | Tripcode: %s", code, Tripcode.tripcode(code)).println();

        Tripcode tc = new Tripcode();
        tc.scanTripcodes("junas");
    }

    public static void main2(String[] args) throws IOException
    {
        Python py = new Python();
        py.generatePasswords(1);
        List<String> first = py.getNextPasswords(10);
        for (String password : first)
            System.out.printf("'%s' ", password);
        System.out.println();
    }

    public static String tripcode(String password)
    {
        if (password == null)
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

    public void scanTripcodes() { this.scanTripcodes(null); }

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
            System.out.println(String.format("> Generating tripcodes with %d-character passwords. Using %d thread(s).", i, Tripcode.cores));

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
