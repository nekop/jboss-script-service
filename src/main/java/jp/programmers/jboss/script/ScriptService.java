package jp.programmers.jboss.script;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import static java.util.logging.Level.*;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ScriptService implements ScriptServiceMBean {

    protected Logger log = Logger.getLogger(getClass().getName());

    protected String scriptDir;
    protected long intervalMillis = 3000;

    protected ExecutorService executor =
        Executors.newSingleThreadExecutor(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return new Thread(r, "ScriptServiceThread");
                }});
    protected ScriptServiceTask task;

    public void create() throws Exception {
        log.fine("ScriptService.create()");
    }

    public void start() throws Exception {
        log.fine("ScriptService.start()");
        if (task != null) {
            // It's already running
            return;
        }
        File dir = new File(scriptDir);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (success) {
                log.log(INFO, "Created scriptDir: {0}", scriptDir);
            } else {
                log.log(WARNING, "Cannot create scriptDir: {0}", scriptDir);
                throw new IllegalStateException(); // TODO
            }
        } else if (dir.exists() && dir.isFile()) {
            log.log(WARNING, "The scriptDir: {0} is not a directory", scriptDir);
            throw new IllegalStateException(); // TODO
        }
        task = new ScriptServiceTask();
        executor.execute(task);
    }

    public void stop() throws Exception {
        log.fine("ScriptService.stop()");
        if (task == null) {
            // It's already stopped
            return;
        }
        task.isRunning = false;
        task.currentThread.interrupt();
    }

    public void destroy() throws Exception {
        log.fine("ScriptService.destroy()");
    }

    public String getScriptDir() {
        return scriptDir;
    }

    public void setScriptDir(String scriptDir) {
        this.scriptDir = scriptDir;
    }

    private class ScriptServiceTask implements Runnable {
        public boolean isRunning = true;
        public Thread currentThread;

        private Map<File, Long> fileMap = new HashMap<File, Long>();
        public void run() {
            currentThread = Thread.currentThread();
            File dir = new File(scriptDir);
            while (isRunning) {
                // TODO: Scan script dir, detect updated deployments
                File[] files = dir.listFiles();
                if (files == null) {
                    log.log(SEVERE, "Cannot read scriptDir: {0}", scriptDir);
                    throw new IllegalStateException(); // TODO
                }
                for (File f : files) {
                    // TODO: Implement ignore file list

                    if (f.getName().startsWith(".") ||
                        f.getName().endsWith("~") ||
                        f.getName().endsWith(".tmp") ||
                        f.getName().endsWith(".bak")) {
                        // ignore
                        continue;
                    }
                        

                    Long lastModified = fileMap.get(f);
                    if (lastModified == null ||
                        lastModified < f.lastModified()) {

                        // Updated, deploy it
                        fileMap.put(f, f.lastModified());

                        String ext = null;
                        if (f.getName().lastIndexOf(".") > 0 &&
                            f.getName().lastIndexOf(".") < f.getName().length()) {
                            ext = f.getName().substring(f.getName().lastIndexOf(".") + 1);
                        } else {
                            log.log(WARNING, "The file {0} doesn't have a file extension in its name", f.getName());
                            continue;
                        }
                        ScriptEngineManager manager = new ScriptEngineManager();
                        ScriptEngine engine = manager.getEngineByExtension(ext);
                        if (engine == null) {
                            log.log(WARNING, "Cannot find script engine for file: {0}", f.getName());
                            continue;
                        }
                        log.log(FINE, "Try to eval file: {0}", f.getName());
                        // TODO: eval, support lifecycle methods

                        FileReader reader = null;
                        try {
                            reader = new FileReader(f);
                            engine.eval(reader);
                        } catch (Exception ex) {
                            LogRecord record = new LogRecord(SEVERE, "Failed to eval file: {0}");
                            record.setParameters(new Object[] {f.getName()});
                            record.setThrown(ex);
                            log.log(record);
                            //log.log(SEVERE, "Failed to eval file: {0}", f.getName());
                        } finally {
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (Exception ignore) { }
                            }

                        }
                    }
                }
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException ignore) {
                    log.fine("Interrupted");
                    isRunning = false;
                }
            }
            // Clear interrupted status
            Thread.interrupted();
        }
    }
}
