package rise;

import rise.http.API;
import rise.peer.Peers;
import rise.user.Users;
import rise.util.Logger;
import rise.util.ThreadPool;
import rise.util.Time;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class Rise {

    public static final String VERSION = "1.0.0";
    public static final String APPLICATION = "RISE";

    private static volatile Time time = new Time.EpochTime();

    private static final Properties defaultProperties = new Properties();
    static {
        System.out.println("Initializing Rise server version " + Rise.VERSION);
        try (InputStream is = ClassLoader.getSystemResourceAsStream("rise-default.properties")) {
            if (is != null) {
                Rise.defaultProperties.load(is);
            } else {
                String configFile = System.getProperty("rise-default.properties");
                if (configFile != null) {
                    try (InputStream fis = new FileInputStream(configFile)) {
                        Rise.defaultProperties.load(fis);
                    } catch (IOException e) {
                        throw new RuntimeException("Error loading rise-default.properties from " + configFile);
                    }
                } else {
                    throw new RuntimeException("rise-default.properties not in classpath and system property rise-default.properties not defined either");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading rise-default.properties", e);
        }
    }
    private static final Properties properties = new Properties(defaultProperties);
    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("rise.properties")) {
            if (is != null) {
                Rise.properties.load(is);
            } // ignore if missing
        } catch (IOException e) {
            throw new RuntimeException("Error loading rise.properties", e);
        }
    }

    public static int getIntProperty(String name) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined, assuming 0");
            return 0;
        }
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null);
    }

    public static String getStringProperty(String name, String defaultValue) {
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value)) {
            Logger.logMessage(name + " = \"" + value + "\"");
            return value;
        } else {
            Logger.logMessage(name + " not defined");
            return defaultValue;
        }
    }

    public static List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public static Boolean getBooleanProperty(String name) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, assuming false");
        return false;
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        Rise.time = time;
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Rise.shutdown();
            }
        }));
        init();
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        init();
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        Logger.logShutdownMessage("Shutting down...");
        API.shutdown();
        Users.shutdown();
        Peers.shutdown();
        ThreadPool.shutdown();
        Db.shutdown();
        Logger.logShutdownMessage("Rise server " + VERSION + " stopped.");
        Logger.shutdown();
    }

    private static class Init {

        static {
            try {
                long startTime = System.currentTimeMillis();
                Logger.init();
                Db.init();
                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();
                Account.init();
                Alias.init();
                Asset.init();
                DigitalGoodsStore.init();
                Hub.init();
                Order.init();
                Poll.init();
                Trade.init();
                AssetTransfer.init();
                Vote.init();
                Peers.init();
                Generator.init();
                MineGenerator.init();
                API.init();
                Users.init();
                DebugTrace.init();
                int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Rise.getIntProperty("rise.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Rise.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    Logger.logMessage("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }

                long currentTime = System.currentTimeMillis();
                Logger.logMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                Logger.logMessage("Rise server " + VERSION + " started successfully.");
                if (Constants.isTestnet) {
                    Logger.logMessage("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
            } catch (Exception e) {
                Logger.logErrorMessage(e.getMessage(), e);
                System.exit(1);
            }
        }

        private static void init() {}

        private Init() {} // never

    }

    private Rise() {} // never

}
