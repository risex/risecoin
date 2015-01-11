package rise;

import rise.crypto.Crypto;
import rise.util.Convert;
import rise.util.Listener;
import rise.util.Listeners;
import rise.util.Logger;
import rise.util.ThreadPool;
import rise.util.MiningPlot;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import fr.cryptohash.Shabal256;

public final class MineGenerator {

    public static enum Event {
        GENERATION_DEADLINE, START_FORGING, STOP_FORGING
    }

    private static final Listeners<MineGenerator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<Long, MineGenerator> generators = new ConcurrentHashMap<>();
    private static final Collection<MineGenerator> allGenerators = Collections.unmodifiableCollection(generators.values());

    private static final Runnable generateBlockThread = new Runnable() {

        @Override
        public void run() {

            try {
                if (Rise.getBlockchainProcessor().isScanning()) {
                    return;
                }
                try {
                    long currentBlock = Rise.getBlockchain().getLastHDDBlock().getHeight();
                    Iterator<Entry<Long, MineGenerator>> it = generators.entrySet().iterator();
                    while(it.hasNext()) {
                    	Entry<Long, MineGenerator> generator = it.next();
                    	if(currentBlock < generator.getValue().getBlock()) {
                    		generator.getValue().forge();
                    	}
                    	else {
                    		it.remove();
                    	}
                    }
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
        ThreadPool.scheduleThread("MineGenerateBlocks", generateBlockThread, 500, TimeUnit.MILLISECONDS);
    }

    static void init() {}

    static void clear() {
    }

    public static boolean addListener(Listener<MineGenerator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<MineGenerator> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Generator startForging(String secretPhrase) {
        return null;
    }
    
    public static MineGenerator addNonce(String secretPhrase, Long nonce) {
    	byte[] publicKey = Crypto.getPublicKey(secretPhrase);
    	return addNonce(secretPhrase, nonce, publicKey);
    }

    public static Generator startForging(String secretPhrase, byte[] publicKey) {
        return null;
    }
    
    public static MineGenerator addNonce(String secretPhrase, Long nonce, byte[] publicKey) {
		byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
		Long id = Convert.fullHashToId(publicKeyHash);
		
		MineGenerator generator = new MineGenerator(secretPhrase, nonce, publicKey, id);
		MineGenerator curGen = generators.get(id);
		if(curGen == null || generator.getBlock() > curGen.getBlock() || generator.getDeadline().compareTo(curGen.getDeadline()) < 0) {
			generators.put(id, generator);
			listeners.notify(generator, Event.START_FORGING);
			Logger.logDebugMessage("Account " + Convert.toUnsignedLong(id) + " started mining, deadline "
			        + generator.getDeadline() + " seconds");
		}
		else {
			Logger.logDebugMessage("Account " + Convert.toUnsignedLong(id) + " already has better nonce");
		}
		
		return generator;
    }

    public static MineGenerator stopForging(String secretPhrase) {
        return null;
    }

    public static MineGenerator getGenerator(String secretPhrase) {
    	return null;
    }

    public static Collection<MineGenerator> getAllGenerators() {
        return allGenerators;
    }

    private final Long accountId;
    private final String secretPhrase;
    private final byte[] publicKey;
    private volatile BigInteger deadline;
    private final long nonce;
    private final long block;

    private MineGenerator(String secretPhrase, Long nonce, byte[] publicKey, Long account) {
        this.secretPhrase = secretPhrase;
        this.publicKey = publicKey;
        // need to store publicKey in addition to accountId, because the account may not have had its publicKey set yet
        this.accountId = account;
        this.nonce = nonce;
        this.block = Rise.getBlockchain().getLastHDDBlock().getHeight() + 1;
        
        // get new generation signature
        Block lastBlock = Rise.getBlockchain().getLastHDDBlock();
		byte[] lastGenSig = lastBlock.getGenerationSignature();
		Long lastGenerator = lastBlock.getGeneratorId();
		
		ByteBuffer gensigbuf = ByteBuffer.allocate(32 + 8);
		gensigbuf.put(lastGenSig);
		gensigbuf.putLong(lastGenerator);
		
		Shabal256 md = new Shabal256();
		md.update(gensigbuf.array());
		byte[] newGenSig = md.digest();
		
		// calculate deadline
		MiningPlot plot = new MiningPlot(accountId, nonce);
		
		ByteBuffer posbuf = ByteBuffer.allocate(32 + 8);
		posbuf.put(newGenSig);
		posbuf.putLong(lastBlock.getHeight() + 1);
		md.reset();
		md.update(posbuf.array());
		BigInteger hashnum = new BigInteger(1, md.digest());
		int scoopNum = hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
		
        md.reset();
        md.update(newGenSig);
        plot.hashScoop(md, scoopNum);
        byte[] hash = md.digest();
        BigInteger hit = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
        
        deadline = hit.divide(BigInteger.valueOf(lastBlock.getBaseTarget()));
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigInteger getDeadline() {
        return deadline;
    }
    
    public long getBlock() {
    	return block;
    }

    private void forge() throws BlockchainProcessor.BlockNotAcceptedException {
        Block lastBlock = Rise.getBlockchain().getLastHDDBlock();

        int elapsedTime = Rise.getEpochTime() - lastBlock.getTimestamp();
        if (BigInteger.valueOf(elapsedTime).compareTo(deadline) > 0) {
            BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, publicKey, nonce);
        }

    }

}
