package rise;

import rise.crypto.Crypto;
import rise.util.Convert;
import rise.util.Listener;
import rise.util.Listeners;
import rise.util.Logger;
import rise.util.ThreadPool;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class Generator implements Comparable<Generator> {

    public static enum Event {
        GENERATION_DEADLINE, START_FORGING, STOP_FORGING
    }

    private static final byte[] fakeForgingPublicKey = Rise.getBooleanProperty("rise.enableFakeForging")
            ? Account.getAccount(Convert.parseAccountId(Rise.getStringProperty("rise.fakeForgingAccount"))).getPublicKey() : null;

    private static final Listeners<Generator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, Generator> generators = new ConcurrentHashMap<>();
    private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());
    private static volatile List<Generator> sortedForgers;

    private static final Runnable generateBlocksThread = new Runnable() {

        private volatile int lastTimestamp;
        private volatile long lastBlockId;

        @Override
        public void run() {

            try {
                try {
                    int timestamp = Rise.getEpochTime();
                    if (timestamp == lastTimestamp) {
                        return;
                    }
                    lastTimestamp = timestamp;
                    synchronized (Rise.getBlockchain()) {
                        Block lastBlock = Rise.getBlockchain().getLastBlock();
                        Block lastPOSBlock = Rise.getBlockchain().getLastPOSBlock();
                        if (lastBlock == null || lastBlock.getHeight() < Constants.LAST_KNOWN_BLOCK) {
                            return;
                        }
                        if (lastBlock.getId() != lastBlockId || sortedForgers == null) {
                            lastBlockId = lastBlock.getId();
                            List<Generator> forgers = new ArrayList<>();
                            for (Generator generator : generators.values()) {
                                generator.setLastBlock(lastPOSBlock);
                                forgers.add(generator);
                            }
                            Collections.sort(forgers);
                            sortedForgers = Collections.unmodifiableList(forgers);
                        }
                        for (Generator generator : sortedForgers) {
                            if (generator.getHitTime() > timestamp + 1 || generator.forge(lastPOSBlock, timestamp)) {
                                return;
                            }
                        }
                    } // synchronized
                } catch (Exception e) {
                    Logger.logDebugMessage("Error in block generation thread", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static {
        ThreadPool.scheduleThread("GenerateBlocks", generateBlocksThread, 500, TimeUnit.MILLISECONDS);
    }

    static void init() {}

    public static boolean addListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Generator> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Generator startForging(String secretPhrase) {
        Generator generator = new Generator(secretPhrase);
        Generator old = generators.putIfAbsent(secretPhrase, generator);
        if (old != null) {
            Logger.logDebugMessage("Account " + Convert.toUnsignedLong(old.getAccountId()) + " is already forging");
            return old;
        }
        listeners.notify(generator, Event.START_FORGING);
        Logger.logDebugMessage("Account " + Convert.toUnsignedLong(generator.getAccountId()) + " started forging, deadline "
                + generator.getDeadline() + " seconds");
        return generator;
    }

    public static Generator stopForging(String secretPhrase) {
        Generator generator = generators.remove(secretPhrase);
        if (generator != null) {
            sortedForgers = null;
            Logger.logDebugMessage("Account " + Convert.toUnsignedLong(generator.getAccountId()) + " stopped forging");
            listeners.notify(generator, Event.STOP_FORGING);
        }
        return generator;
    }

    public static Generator getGenerator(String secretPhrase) {
        return generators.get(secretPhrase);
    }

    public static Collection<Generator> getAllGenerators() {
        return allGenerators;
    }

    static boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            return false;
        }
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(effectiveBalance);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        return hit.compareTo(target) < 0;
    }

    static long getHitTime(Account account, Block block) {
        return getHitTime(BigInteger.valueOf(account.getEffectiveBalanceRISE()), getHit(account.getId(), block), block);
    }

    static boolean allowsFakeForging(byte[] publicKey) {
        return Constants.isTestnet && publicKey != null && Arrays.equals(publicKey, fakeForgingPublicKey);
    }

    private static BigInteger getHit(long id, Block block) {
        MessageDigest digest = Crypto.sha256();
        ByteBuffer idBytes = ByteBuffer.allocate(8);
        idBytes.putLong(id);
        idBytes.flip();
        digest.update(block.getGenerationSignature());
        byte[] generationSignatureHash = digest.digest(idBytes.array());
        return new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
    }

    private static long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block) {
        return block.getTimestamp()
                + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance)).longValue();
    }


    private final long accountId;
    private final String secretPhrase;
    private final byte[] publicKey;
    private volatile long hitTime;
    private volatile BigInteger hit;
    private volatile BigInteger effectiveBalance;

    private Generator(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.accountId = Account.getId(publicKey);
        setLastBlock(Rise.getBlockchain().getLastPOSBlock());
        sortedForgers = null;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getDeadline() {
        return Math.max(hitTime - Rise.getBlockchain().getLastPOSBlock().getTimestamp(), 0);
    }

    public long getHitTime() {
        return hitTime;
    }

    @Override
    public int compareTo(Generator g) {
        int i = this.hit.multiply(g.effectiveBalance).compareTo(g.hit.multiply(this.effectiveBalance));
        if (i != 0) {
            return i;
        }
        return Long.compare(accountId, g.accountId);
    }

    @Override
    public String toString() {
        return "account: " + Convert.toUnsignedLong(accountId) + " deadline: " + getDeadline();
    }

    private void setLastBlock(Block lastBlock) {
        Account account = Account.getAccount(accountId);
        effectiveBalance = BigInteger.valueOf(account == null || account.getEffectiveBalanceRISE() <= 0 ? 0 : account.getEffectiveBalanceRISE());
        hit = getHit(accountId, lastBlock);
        hitTime = getHitTime(effectiveBalance, hit, lastBlock);
        listeners.notify(this, Event.GENERATION_DEADLINE);
    }

    private boolean forge(Block lastBlock, int timestamp) throws BlockchainProcessor.BlockNotAcceptedException {
        if (verifyHit(hit, effectiveBalance, lastBlock, timestamp)) {
            while (true) {
                try {
                    BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, publicKey, -1L);
                    return true;
                } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                    if (Rise.getEpochTime() - timestamp > 10) {
                        throw e;
                    }
                }
            }
        }
        return false;
    }

}
