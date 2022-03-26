/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


/**
 * Utility class to read the bootstrap Catalina configuration.
 *
 * @author Remy Maucherat
 */
public class CatalinaProperties {

    private static final Log log = LogFactory.getLog(CatalinaProperties.class);

    private static Properties properties = null;


    /*
     * CatalinaProperties这个类加载的时候，
     * 就读取配置文件，并转换properties
     */
    static {
        loadProperties();
    }


    /**
     * @param name The property name
     * @return specified property value
     */
    public static String getProperty(String name) {
        return properties.getProperty(name);
    }


    /**
     * Load properties.
     */
    private static void loadProperties() {

        InputStream is = null;
        String fileName = "catalina.properties";

        // 优先 尝试读取系统变量catalina.config指定的配置文件
        try {
            String configUrl = System.getProperty("catalina.config");
            if (configUrl != null) {
                if (configUrl.indexOf('/') == -1) {
                    // No '/'. Must be a file name rather than a URL
                    fileName = configUrl;
                } else {
                    is = (new URL(configUrl)).openStream();
                }
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        if (is == null) {
            try {
                File home = new File(Bootstrap.getCatalinaBase());
                // Tomcat zip包解压后，进入就有一个conf目录
                // conf目录下面，就有一个catalina.properties文件
                File conf = new File(home, "conf"); // conf目录
                File propsFile = new File(conf, fileName); // conf目录下的catalina.properties文件
                is = new FileInputStream(propsFile);
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        if (is == null) {
            try {
                is = CatalinaProperties.class.getResourceAsStream
                    ("/org/apache/catalina/startup/catalina.properties");
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        if (is != null) {
            try {
                properties = new Properties();
                /*
                 * !!!!!!!!!!!!!!!!!!!!!!
                 *
                 * 全世界读取properties文件都是这套路，都是一样的。
                 * 都是读取文件，转为inputStream，然后，再使用Properties的load方法，从inputStream读取内容，填充properties对象。
                 *
                 * !!!!!!!!!!!!!!!!!!!!!!
                 */
                properties.load(is);
            } catch (Throwable t) {
                handleThrowable(t);
                log.warn(t);
            } finally {
                try {
                    is.close();
                } catch (IOException ioe) {
                    log.warn("Could not close catalina properties file", ioe);
                }
            }
        }

        if ((is == null)) {
            // Do something
            log.warn("Failed to load catalina properties file");
            // That's fine - we have reasonable defaults.
            properties = new Properties();
        }

        // Register the properties as system properties
        Enumeration<?> enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();
            String value = properties.getProperty(name);
            if (value != null) {
                System.setProperty(name, value);
            }
        }
    }


    // Copied from ExceptionUtils since that class is not visible during start
    private static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }
}
