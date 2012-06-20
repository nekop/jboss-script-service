package jp.programmers.jboss.script;

import java.util.Map;
import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ScriptService implements ScriptServiceMBean {

    protected Logger log = Logger.getLogger(getClass().getName());

    protected String scriptDir;
    protected long intervalMillis = 3000;

    protected ScriptEngineManager manager = new ScriptEngineManager();
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
                log.log(Level.FINE, "Created scriptDir: %s", scriptDir);
            } else {
                log.log(Level.WARNING, "Cannot create scriptDir: %s", scriptDir);
                throw new IllegalStateException(); // TODO
            }
        } else if (dir.exists() && dir.isFile()) {
            log.log(Level.WARNING, "The scriptDir: %s is not a directory", scriptDir);
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
                for (File f : files) {
                    Long lastModified = fileMap.get(f);
                    if (lastModified == null ||
                        lastModified < f.lastModified()) {
                        // Updated, deploy it
                        if (f.getName().lastIndexOf(".") < 0) {
                            log.log(Level.WARNING, "The file %s doesn't have a file extension in its name", f.getName());
                            continue;
                        }
                        ScriptEngine engine =
                            manager.getEngineByExtension(f.getName());
                        if (engine == null) {
                            log.log(Level.WARNING, "Cannot find script engine for file: %s", f.getName());
                            continue;
                        }
                        log.log(Level.FINE, "Try to eval file: %s", f.getName());
                        // TODO: eval, support lifecycle methods
                        fileMap.put(f, f.lastModified());
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
            currentThread.interrupted();
        }
    }
}
