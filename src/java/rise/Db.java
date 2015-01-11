package rise;

import rise.db.BasicDb;
import rise.db.TransactionalDb;

public final class Db {

    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Rise.getIntProperty("rise.dbCacheKB"))
            .dbUrl(Constants.isTestnet ? Rise.getStringProperty("rise.testDbUrl") : Rise.getStringProperty("rise.dbUrl"))
            .maxConnections(Rise.getIntProperty("rise.maxDbConnections"))
            .loginTimeout(Rise.getIntProperty("rise.dbLoginTimeout"))
            .defaultLockTimeout(Rise.getIntProperty("rise.dbDefaultLockTimeout") * 1000)
    );

    /*
    public static final BasicDb userDb = new BasicDb(new BasicDb.DbProperties()
            .maxCacheSize(Rise.getIntProperty("rise.userDbCacheKB"))
            .dbUrl(Constants.isTestnet ? Rise.getStringProperty("rise.testUserDbUrl") : Rise.getStringProperty("rise.userDbUrl"))
            .maxConnections(Rise.getIntProperty("rise.maxUserDbConnections"))
            .loginTimeout(Rise.getIntProperty("rise.userDbLoginTimeout"))
            .defaultLockTimeout(Rise.getIntProperty("rise.userDbDefaultLockTimeout") * 1000)
    );
    */

    static void init() {
        db.init("sa", "sa", new RiseDbVersion());
        //userDb.init("sa", "databaseencryptionpassword sa", new UserDbVersion());
    }

    static void shutdown() {
        //userDb.shutdown();
        db.shutdown();
    }

    private Db() {} // never

}
