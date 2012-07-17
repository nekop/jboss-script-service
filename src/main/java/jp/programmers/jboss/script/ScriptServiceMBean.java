package jp.programmers.jboss.script;

import java.util.logging.Logger;

public interface ScriptServiceMBean {

    public void create() throws Exception;
    public void start() throws Exception;
    public void stop() throws Exception;
    public void destroy() throws Exception;

    public String getScriptDir();
    public void setScriptDir(String scriptDir);
}
