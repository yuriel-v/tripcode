package dev.yuriel.spiceworks.tripcode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Vector;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Hello world!
 *
 */
public class Tripcode 
{
    private static final String saltTable = ".............................................../0123456789ABCDEFGABCDEFGHIJKLMNOPQRSTUVWXYZabcdefabcdefghijklmnopqrstuvwxyz.....................................................................................................................................";
    private static final String suffix = "H.";
    private static final String charTable = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ !@$%*()[]{}\\-=_+;:/?,.|";
    private PrintStream ps;
    private final Gson gson = new Gson();
    private static final short cores = 6;

    public static void main2(String[] args)
    {
        //String code = "a";
        //System.out.printf("Password: %s | Tripcode: %s", code, Tripcode.tripcode(code)).println();

        Tripcode tc = new Tripcode();
        tc.scanTripcodes();
    }

    public static void main(String[] args) throws IOException
    {
        Python py = new Python();
        System.out.println(py.sendCommand("print(f'2 to the power of 2 is {2 ** 2}')"));
    }

    public static String tripcode(String password)
    {
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
        try
        {
            if (pattern == null)
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

            System.out.println("-> Fetching passwords list: " + i + "-character long.");
            String p = this.getPasswords(i);
            Vector<String> pws = gson.fromJson(p, new TypeToken<Vector<String>>(){}.getType());
            p = null;
            int n = (int) Math.ceil(pws.size() / Tripcode.cores);
            Vector<Vector<String>> passwords = new Vector<Vector<String>>();

            int k = 0;
            for (int j = 0; j < Tripcode.cores; ++j) {
                Vector<String> newVec = new Vector<String>(pws.subList(k, k+n > pws.size()? pws.size() : k+n));
                passwords.add(newVec);
                pws.removeAll(newVec);
                k += n;
            }
            pws = null;
            System.out.println(String.format("  > Generating with passwords of length %d of 8. Using %d thread(s).", i, Tripcode.cores));
            
            for (int j = 0; j < Tripcode.cores; ++j) {
                Runnable worker = new TripcodePrinter(this.ps, passwords.get(j));
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

    private String getPasswords(int line)
    {
        File products = new File("products.txt");
        if (!products.exists())
        {
            System.out.println("(!) Attempting to generate character table cartesian product.");
            try {
                Process p = new ProcessBuilder("/bin/python3", "./tripcode/product.py", "8", Tripcode.charTable).start();
                //Process p = new ProcessBuilder("pwd").start();
                p.waitFor();
                String stdout = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                return stdout;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
        String product = null;
        try {
            Stream<String> allLines = Files.lines(products.toPath());
            product = allLines.skip(line == 0? line : line-1).findFirst().get();
            allLines.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return product;
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
