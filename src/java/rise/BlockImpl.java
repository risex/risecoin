package rise;

import rise.BlockchainProcessor.BlockNotAcceptedException;
import rise.TransactionImpl.BuilderImpl;
import rise.crypto.Crypto;
import rise.util.Convert;
import rise.util.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rise.util.MiningPlot;
import fr.cryptohash.Shabal256;

final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final long previousBlockId;
    private final byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final long totalAmountNQT;
    private final long totalFeeNQT;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private volatile List<TransactionImpl> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private long baseTarget = Constants.INITIAL_HDD_BASE_TARGET;
    private volatile long nextBlockId;
    private int height = -1;
    private volatile long id;
    private volatile String stringId = null;
    private volatile long generatorId;
    private long nonce;


    BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions, long nonce)
            throws RiseException.ValidationException {

        if (payloadLength > Constants.MAX_PAYLOAD_LENGTH || payloadLength < 0) {
            throw new RiseException.NotValidException("attempted to create a block with payloadLength " + payloadLength);
        }

        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        this.totalAmountNQT = totalAmountNQT;
        this.totalFeeNQT = totalFeeNQT;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;
        this.previousBlockHash = previousBlockHash;
        if (transactions != null) {
            this.blockTransactions = Collections.unmodifiableList(transactions);
            if (blockTransactions.size() > Constants.MAX_NUMBER_OF_TRANSACTIONS) {
                throw new RiseException.NotValidException("attempted to create a block with " + blockTransactions.size() + " transactions");
            }
            long previousId = 0;
            for (Transaction transaction : this.blockTransactions) {
                if (transaction.getId() <= previousId && previousId != 0) {
                    throw new RiseException.NotValidException("Block transactions are not sorted!");
                }
                previousId = transaction.getId();
            }
        }
        this.nonce = nonce;
    }

    BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength,
              byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long nextBlockId, int height, long id, long nonce)
            throws RiseException.ValidationException {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, blockSignature, previousBlockHash, null, nonce);
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.baseTarget = baseTarget;
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public long getTotalAmountNQT() {
        return totalAmountNQT;
    }

    @Override
    public long getTotalFeeNQT() {
        return totalFeeNQT;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<TransactionImpl> getTransactions() {
        if (blockTransactions == null) {
            this.blockTransactions = Collections.unmodifiableList(TransactionDb.findBlockTransactions(getId()));
            for (TransactionImpl transaction : this.blockTransactions) {
                transaction.setBlock(this);
            }
        }
        return blockTransactions;
    }

    @Override
    public long getBaseTarget() {
        return baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    @Override
    public long getNextBlockId() {
        return nextBlockId;
    }

    @Override
    public int getHeight() {
        if (height == -1) {
            throw new IllegalStateException("Block height not yet set");
        }
        return height;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
            byte[] hash = Crypto.sha256().digest(getBytes());
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Convert.toUnsignedLong(id);
            }
        }
        return stringId;
    }

    @Override
    public long getGeneratorId() {
        if (generatorId == 0) {
            generatorId = Account.getId(generatorPublicKey);
        }
        return generatorId;
    }
    
    @Override
    public Long getNonce() {
    	return nonce;
    }
    
    @Override
    public int getScoopNum() {
    	ByteBuffer posbuf = ByteBuffer.allocate(32 + 8);
		posbuf.put(generationSignature);
		posbuf.putLong(getHeight());
		Shabal256 md = new Shabal256();
		md.update(posbuf.array());
		BigInteger hashnum = new BigInteger(1, md.digest());
		int scoopNum = hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
		return scoopNum;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId() == ((BlockImpl)o).getId();
    }

    @Override
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("timestamp", timestamp);
        json.put("previousBlock", Convert.toUnsignedLong(previousBlockId));
        json.put("totalAmountNQT", totalAmountNQT);
        json.put("totalFeeNQT", totalFeeNQT);
        json.put("payloadLength", payloadLength);
        json.put("payloadHash", Convert.toHexString(payloadHash));
        json.put("generatorPublicKey", Convert.toHexString(generatorPublicKey));
        json.put("generationSignature", Convert.toHexString(generationSignature));
        if (version > 1) {
            json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        }
        json.put("blockSignature", Convert.toHexString(blockSignature));
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : getTransactions()) {
            transactionsData.add(transaction.getJSONObject());
        }
        json.put("transactions", transactionsData);
        json.put("nonce", Convert.toUnsignedLong(nonce));
        return json;
    }

    static BlockImpl parseBlock(JSONObject blockData) throws RiseException.ValidationException {
        try {
            int version = ((Long) blockData.get("version")).intValue();
            int timestamp = ((Long) blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            long totalAmountNQT = Convert.parseLong(blockData.get("totalAmountNQT"));
            long totalFeeNQT = Convert.parseLong(blockData.get("totalFeeNQT"));
            int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
            List<TransactionImpl> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(TransactionImpl.parseTransaction((JSONObject) transactionData));
            }
            Long nonce = Convert.parseUnsignedLong((String)blockData.get("nonce"));
            return new BlockImpl(version, timestamp, previousBlock, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey,
                    generationSignature, blockSignature, previousBlockHash, blockTransactions, nonce);
        } catch (RiseException.ValidationException|RuntimeException e) {
            Logger.logDebugMessage("Failed to parse block: " + blockData.toJSONString());
            throw e;
        }
    }

    byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + (version < 3 ? (4 + 4) : (8 + 8)) + 4 + 32 + 32 + (32 + 32) + 8 + 64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.putInt(timestamp);
        buffer.putLong(previousBlockId);
        buffer.putInt(getTransactions().size());
        if (version < 3) {
            buffer.putInt((int)(totalAmountNQT / Constants.ONE_RISE));
            buffer.putInt((int)(totalFeeNQT / Constants.ONE_RISE));
        } else {
            buffer.putLong(totalAmountNQT);
            buffer.putLong(totalFeeNQT);
        }
        buffer.putInt(payloadLength);
        buffer.put(payloadHash);
        buffer.put(generatorPublicKey);
        buffer.put(generationSignature);
        if (version > 1) {
            buffer.put(previousBlockHash);
        }
        buffer.putLong(nonce);
        buffer.put(blockSignature);
        return buffer.array();
    }

    void sign(String secretPhrase) {
        if (blockSignature != null) {
            throw new IllegalStateException("Block already signed");
        }
        blockSignature = new byte[64];
        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);
        blockSignature = Crypto.sign(data2, secretPhrase);
    }

    boolean verifyBlockSignature() throws BlockchainProcessor.BlockOutOfOrderException {

    	try {
    		
    		if(getNonce() == -1) {
    			Account account = Account.getAccount(getGeneratorId());
    			if (account == null) {
    	            return false;
    	        }
    			
    			byte[] data = getBytes();
    	        byte[] data2 = new byte[data.length - 64];
    	        System.arraycopy(data, 0, data2, 0, data2.length);

    	        return Crypto.verify(blockSignature, data2, generatorPublicKey, version >= 3) && account.setOrVerify(generatorPublicKey, this.height);
    		}
    		else {
	    		BlockImpl previousBlock = (BlockImpl)Rise.getBlockchain().getBlock(this.previousBlockId);
	    		if (previousBlock == null) {
	                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing");
	            }
	    		
	    		byte[] data = getBytes();
	            byte[] data2 = new byte[data.length - 64];
	            System.arraycopy(data, 0, data2, 0, data2.length);
	            
	            byte[] publicKey;
	            Account genAccount = Account.getAccount(generatorPublicKey);
	            Account.RewardRecipientAssignment rewardAssignment;
	            rewardAssignment = genAccount == null ? null : genAccount.getRewardRecipientAssignment();
	            if(genAccount == null ||
	               rewardAssignment == null) {
	            	publicKey = generatorPublicKey;
	            }
	            else {
	            	if(previousBlock.getHeight() + 1 >= rewardAssignment.getFromHeight()) {
	            		publicKey = Account.getAccount(rewardAssignment.getRecipientId()).getPublicKey();
	            	}
	            	else {
	            		publicKey = Account.getAccount(rewardAssignment.getPrevRecipientId()).getPublicKey();
	            	}
	            }
	
	            return Crypto.verify(blockSignature, data2, publicKey, version >= 3);
    		}
    		
    	} catch (RuntimeException e) {

    		Logger.logMessage("Error verifying block signature", e);
    		return false;

    	}
    }

    boolean verifyGenerationSignature() throws BlockchainProcessor.BlockNotAcceptedException {

    	try {

    		if(getNonce() == -1) {
    			BlockImpl previousBlock = (BlockImpl) Rise.getBlockchain().getBlock(this.previousBlockId);
    			if (previousBlock == null) {
                    throw new BlockchainProcessor.BlockNotAcceptedException("Can't verify signature because previous block is missing");
                }

                Account account = Account.getAccount(getGeneratorId());
                long effectiveBalance = account == null ? 0 : account.getEffectiveBalanceRISE();
                if (effectiveBalance <= 0) {
                    return false;
                }

                MessageDigest digest = Crypto.sha256();
                byte[] generationSignatureHash;
                digest.update(Rise.getBlockchain().getLastPOSBlock().getGenerationSignature());
                ByteBuffer idBytes = ByteBuffer.allocate(8);
                idBytes.putLong(account.getId());
                idBytes.flip();
                generationSignatureHash = digest.digest(idBytes.array());
                if (!Arrays.equals(generationSignature, generationSignatureHash)) {
                	return false;
                }

                BigInteger hit = new BigInteger(1, new byte[]{generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

                return Generator.verifyHit(hit, BigInteger.valueOf(effectiveBalance), Rise.getBlockchain().getLastPOSBlock(), timestamp);
    		}
    		else {
    			if(Rise.getBlockchain().getHeight() >= 2365200) {
    				throw new BlockchainProcessor.BlockNotAcceptedException("POC no longer enabled");
    			}
	    		BlockImpl previousBlock = (BlockImpl)Rise.getBlockchain().getBlock(this.previousBlockId);
	            if (previousBlock == null) {
	                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify generation signature because previous block is missing");
	            }
	            BlockImpl previousHDDBlock = (BlockImpl)Rise.getBlockchain().getLastHDDBlock();
	
	            //Account account = Account.getAccount(getGeneratorId());
	
	            ByteBuffer gensigbuf = ByteBuffer.allocate(32 + 8);
	            gensigbuf.put(previousHDDBlock.getGenerationSignature());
	            gensigbuf.putLong(previousHDDBlock.getGeneratorId());
	            
	            Shabal256 md = new Shabal256();
	            md.update(gensigbuf.array());
	            byte[] correctGenerationSignature = md.digest();
	            if(!Arrays.equals(generationSignature, correctGenerationSignature)) {
	            	return false;
	            }
	            
	            // verify poc also
	            MiningPlot plot = new MiningPlot(getGeneratorId(), nonce);
	            
	            ByteBuffer posbuf = ByteBuffer.allocate(32 + 8);
	    		posbuf.put(correctGenerationSignature);
	    		posbuf.putLong(previousHDDBlock.getHeight() + 1);
	    		md.reset();
	    		md.update(posbuf.array());
	    		BigInteger hashnum = new BigInteger(1, md.digest());
	    		int scoopNum = hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
	    		
	    		md.reset();
	            md.update(correctGenerationSignature);
	            plot.hashScoop(md, scoopNum);
	            byte[] hash = md.digest();
	            BigInteger hit = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
	            BigInteger hitTime = hit.divide(BigInteger.valueOf(previousHDDBlock.getBaseTarget()));
	            
	            int elapsedTime = timestamp - previousHDDBlock.timestamp;
	            
	            return BigInteger.valueOf(elapsedTime).compareTo(hitTime) > 0;
    		}
    		
        } catch (RuntimeException e) {
        	e.printStackTrace(System.out);
            Logger.logMessage("Error verifying block generation signature", e);
            return false;

        }

    }

    void apply() {
    	if(this.nonce == -1) {
    		Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
            generatorAccount.apply(generatorPublicKey, this.height);
            generatorAccount.addToBalanceAndUnconfirmedBalanceNQT(totalFeeNQT + 2 * Constants.ONE_RISE);
            generatorAccount.addToForgedBalanceNQT(totalFeeNQT + 2 * Constants.ONE_RISE);
            
            generatorAccount.addSelfTx(generatorAccount.getBalanceNQT(), getId(), getTimestamp());
            
            for (TransactionImpl transaction : getTransactions()) {
                transaction.apply();
            }
    	}
    	else {
    		Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
            generatorAccount.apply(generatorPublicKey, this.height);

        	Account rewardAccount;
        	Account.RewardRecipientAssignment rewardAssignment = generatorAccount.getRewardRecipientAssignment();
        	if(rewardAssignment == null) {
        		rewardAccount = generatorAccount;
        	}
        	else if(height >= rewardAssignment.getFromHeight()) {
        		rewardAccount = Account.getAccount(rewardAssignment.getRecipientId());
        	}
        	else {
        		rewardAccount = Account.getAccount(rewardAssignment.getPrevRecipientId());
        	}
        	rewardAccount.addToBalanceAndUnconfirmedBalanceNQT(totalFeeNQT + 217 * Constants.ONE_RISE);
        	rewardAccount.addToForgedBalanceNQT(totalFeeNQT + 217 * Constants.ONE_RISE);
        	
        	rewardAccount.addSelfTx(totalFeeNQT + 217 * Constants.ONE_RISE, getId(), getTimestamp());

            for (TransactionImpl transaction : getTransactions()) {
                transaction.apply();
            }
    	}
    }

    void setPrevious(BlockImpl block) throws BlockNotAcceptedException {
        if (block != null) {
            if (block.getId() != getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = block.getHeight() + 1;
            try {
            	this.calculateBaseTarget(block);
            }
            catch(SQLException e) {
            	throw new BlockchainProcessor.BlockNotAcceptedException("Failed to calc basetarget");
            }
            
        } else {
            this.height = 0;
        }
        for (TransactionImpl transaction : getTransactions()) {
            transaction.setBlock(this);
        }
    }
    
    private long calculateMinDestroyedCoinAge() {
    	HashMap<Long, Long> coinsUsed = new HashMap<>();
    	for(TransactionImpl transaction : getTransactions()) {
    		Long usedAmount = coinsUsed.get(transaction.getSenderId());
    		if(usedAmount == null)
    			usedAmount = 0L;
    		usedAmount = usedAmount + transaction.getAmountNQT();
    		coinsUsed.put(transaction.getSenderId(), usedAmount);
    	}
    	if(this.nonce == -1) {
    		Account genAccount = Account.getAccount(getGeneratorId());
        	if(genAccount != null) {
        		coinsUsed.put(genAccount.getId(), genAccount.getBalanceNQT());
        	}
    	}
    	
    	long ageDestroyed = 0;
    	for(Long accountId : coinsUsed.keySet()) {
    		Account account = Account.getAccount(accountId);
    		if(account != null) {
    			ageDestroyed += account.getEffectiveBalanceRISE(coinsUsed.get(accountId));
    		}
    	}
    	return ageDestroyed;
    }

    private void calculateBaseTarget(BlockImpl previousBlock) throws SQLException {
    	
    	if(this.nonce == -1) {
    		if (this.getId() == Genesis.GENESIS_BLOCK_ID && previousBlockId == 0) {
	            baseTarget = Constants.INITIAL_POS_BASE_TARGET;
	            cumulativeDifficulty = BigInteger.ZERO;
	        } else if(this.height < 4) {
	        	baseTarget = Constants.INITIAL_POS_BASE_TARGET;
	        	cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(BigInteger.valueOf(calculateMinDestroyedCoinAge()));
	        } else if(this.height < Constants.DIFF_ADJUST_CHANGE_BLOCK){
	        	BigInteger avgBaseTarget = BigInteger.ZERO;
	        	int oldTimestamp = 0;
	        	int numBlocks = 0;
	        	try(Connection con = Db.db.getConnection();
	        			PreparedStatement pstmt = con.prepareStatement("SELECT base_target, timestamp FROM block "
	        					+ "WHERE nonce = ? "
	        					+ "ORDER BY height DESC LIMIT 4")) {
	        		pstmt.setLong(1, -1);
	        		ResultSet rs = pstmt.executeQuery();
	        		while(rs.next()) {
	        			avgBaseTarget = avgBaseTarget.add(BigInteger.valueOf(rs.getLong("base_target")));
	        			oldTimestamp = rs.getInt("timestamp");
	        			numBlocks++;
	        		}
	        	}
	        	if(numBlocks != 0)
	        		avgBaseTarget = avgBaseTarget.divide(BigInteger.valueOf(numBlocks));
	        	else
	        		avgBaseTarget = BigInteger.valueOf(Constants.INITIAL_POS_BASE_TARGET);
	        	long difTime = this.timestamp - oldTimestamp;
	        	
	            long curBaseTarget = avgBaseTarget.longValue();
	            long newBaseTarget = BigInteger.valueOf(curBaseTarget)
	                    .multiply(BigInteger.valueOf(difTime))
	                    .divide(BigInteger.valueOf(30 * 4)).longValue();
	            if (newBaseTarget < (curBaseTarget * 9 / 10)) {
	            	newBaseTarget = curBaseTarget * 9 / 10;
	            }
	            if (newBaseTarget == 0) {
	                newBaseTarget = 1;
	            }
	            long twofoldCurBaseTarget = curBaseTarget * 11 / 10;
	            if (newBaseTarget > twofoldCurBaseTarget) {
	                newBaseTarget = twofoldCurBaseTarget;
	            }
	            baseTarget = newBaseTarget;
	            cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(BigInteger.valueOf(calculateMinDestroyedCoinAge()));
	        }
	        else {
	        	BigInteger avgBaseTarget = BigInteger.ZERO;
	        	int blockCounter = 1;
	        	int oldTimestamp = 0;
	        	try(Connection con = Db.db.getConnection();
	        			PreparedStatement pstmt = con.prepareStatement("SELECT base_target, timestamp FROM block "
	        					+ "WHERE nonce = ? "
	        					+ "ORDER BY height DESC LIMIT 24")) {
	        		pstmt.setLong(1, -1);
	        		ResultSet rs = pstmt.executeQuery();
	        		while(rs.next()) {
	        			blockCounter++;
	        			if(blockCounter == 2) {
	        				avgBaseTarget = avgBaseTarget.add(BigInteger.valueOf(rs.getLong("base_target")));
	        			}
	        			else {
	        				avgBaseTarget = (avgBaseTarget.multiply(BigInteger.valueOf(blockCounter))
        							.add(BigInteger.valueOf(rs.getLong("base_target"))))
        							.divide(BigInteger.valueOf(blockCounter + 1));
	        			}
	        			oldTimestamp = rs.getInt("timestamp");
	        		}
	        	}
	        	long difTime = this.timestamp - oldTimestamp;
	        	long targetTimespan = 24 * 30;
	        	
	        	if(difTime < targetTimespan /2) {
	        		difTime = targetTimespan /2;
	        	}
	        	
	        	if(difTime > targetTimespan * 2) {
	        		difTime = targetTimespan * 2;
	        	}
	        	
	        	long curBaseTarget = Rise.getBlockchain().getLastPOSBlock().getBaseTarget();
	            long newBaseTarget = avgBaseTarget
	                    .multiply(BigInteger.valueOf(difTime))
	                    .divide(BigInteger.valueOf(targetTimespan)).longValue();
	            
	            if (newBaseTarget == 0) {
	                newBaseTarget = 1;
	            }
	            
	            if(newBaseTarget < curBaseTarget * 8 / 10) {
	            	newBaseTarget = curBaseTarget * 8 / 10;
	            }
	            
	            if(newBaseTarget > curBaseTarget * 12 / 10) {
	            	newBaseTarget = curBaseTarget * 12 / 10;
	            }
	            
	            baseTarget = newBaseTarget;
	            cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(BigInteger.valueOf(calculateMinDestroyedCoinAge()));
	        }
    	}
    	else {
	    	if (this.getId() == Genesis.GENESIS_BLOCK_ID && previousBlockId == 0) {
	            baseTarget = Constants.INITIAL_HDD_BASE_TARGET;
	            cumulativeDifficulty = BigInteger.ZERO;
	        } else if(this.height < 4) {
	        	baseTarget = Constants.INITIAL_HDD_BASE_TARGET;
	        	cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(BigInteger.valueOf(calculateMinDestroyedCoinAge() + 217));
	        } else if(this.height < Constants.DIFF_ADJUST_CHANGE_BLOCK){
	        	BigInteger avgBaseTarget = BigInteger.ZERO;
	        	int oldTimestamp = 0;
	        	int numBlocks = 0;
	        	try(Connection con = Db.db.getConnection();
	        			PreparedStatement pstmt = con.prepareStatement("SELECT base_target, timestamp FROM block "
	        					+ "WHERE nonce <> ? "
	        					+ "ORDER BY height DESC LIMIT 4")) {
	        		pstmt.setLong(1, -1);
	        		ResultSet rs = pstmt.executeQuery();
	        		while(rs.next()) {
	        			avgBaseTarget = avgBaseTarget.add(BigInteger.valueOf(rs.getLong("base_target")));
	        			oldTimestamp = rs.getInt("timestamp");
	        			numBlocks++;
	        		}
	        	}
	        	if(numBlocks != 0)
	        		avgBaseTarget = avgBaseTarget.divide(BigInteger.valueOf(numBlocks));
	        	else
	        		avgBaseTarget = BigInteger.valueOf(Constants.INITIAL_HDD_BASE_TARGET);
	        	long difTime = this.timestamp - oldTimestamp;
	        	
	            long curBaseTarget = avgBaseTarget.longValue();
	            long newBaseTarget = BigInteger.valueOf(curBaseTarget)
	                    .multiply(BigInteger.valueOf(difTime))
	                    .divide(BigInteger.valueOf(240 * 4)).longValue();
	            if (newBaseTarget < (curBaseTarget * 9 / 10)) {
	            	newBaseTarget = curBaseTarget * 9 / 10;
	            }
	            if (newBaseTarget == 0) {
	                newBaseTarget = 1;
	            }
	            long twofoldCurBaseTarget = curBaseTarget * 11 / 10;
	            if (newBaseTarget > twofoldCurBaseTarget) {
	                newBaseTarget = twofoldCurBaseTarget;
	            }
	            baseTarget = newBaseTarget;
	            cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(BigInteger.valueOf(calculateMinDestroyedCoinAge() + 217));
	        }
	        else {
	        	BigInteger avgBaseTarget = BigInteger.ZERO;
	        	int blockCounter = 1;
	        	int oldTimestamp = 0;
	        	try(Connection con = Db.db.getConnection();
	        			PreparedStatement pstmt = con.prepareStatement("SELECT base_target, timestamp FROM block "
	        					+ "WHERE nonce <> ? "
	        					+ "ORDER BY height DESC LIMIT 24")) {
	        		pstmt.setLong(1, -1);
	        		ResultSet rs = pstmt.executeQuery();
	        		while(rs.next()) {
	        			blockCounter++;
	        			if(blockCounter == 2) {
	        				avgBaseTarget = avgBaseTarget.add(BigInteger.valueOf(rs.getLong("base_target")));
	        			}
	        			else {
	        				avgBaseTarget = (avgBaseTarget.multiply(BigInteger.valueOf(blockCounter))
        							.add(BigInteger.valueOf(rs.getLong("base_target"))))
        							.divide(BigInteger.valueOf(blockCounter + 1));
	        			}
	        			oldTimestamp = rs.getInt("timestamp");
	        		}
	        	}
	        	long difTime = this.timestamp - oldTimestamp;
	        	long targetTimespan = 24 * 4 * 60;
	        	
	        	if(difTime < targetTimespan /2) {
	        		difTime = targetTimespan /2;
	        	}
	        	
	        	if(difTime > targetTimespan * 2) {
	        		difTime = targetTimespan * 2;
	        	}
	        	
	        	long curBaseTarget = Rise.getBlockchain().getLastHDDBlock().getBaseTarget();
	            long newBaseTarget = avgBaseTarget
	                    .multiply(BigInteger.valueOf(difTime))
	                    .divide(BigInteger.valueOf(targetTimespan)).longValue();
	            
	            if (newBaseTarget == 0) {
	                newBaseTarget = 1;
	            }
	            
	            if(newBaseTarget < curBaseTarget * 8 / 10) {
	            	newBaseTarget = curBaseTarget * 8 / 10;
	            }
	            
	            if(newBaseTarget > curBaseTarget * 12 / 10) {
	            	newBaseTarget = curBaseTarget * 12 / 10;
	            }
	            
	            baseTarget = newBaseTarget;
	            cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(BigInteger.valueOf(calculateMinDestroyedCoinAge() + 217));
	        }
    	}
    }

}
