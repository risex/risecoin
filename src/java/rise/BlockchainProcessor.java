package rise;

import rise.db.DerivedDbTable;
import rise.peer.Peer;
import rise.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface BlockchainProcessor extends Observable<Block,BlockchainProcessor.Event> {

    public static enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED, BLOCK_SCANNED,
        RESCAN_BEGIN, RESCAN_END,
        BEFORE_BLOCK_ACCEPT,
        BEFORE_BLOCK_APPLY, AFTER_BLOCK_APPLY
    }

    Peer getLastBlockchainFeeder();

    int getLastBlockchainFeederHeight();

    boolean isScanning();

    int getMinRollbackHeight();

    void processPeerBlock(JSONObject request) throws RiseException;

    void fullReset();

    void scan(int height);

    void forceScanAtStart();

    void validateAtNextScan();

    void setGetMoreBlocks(boolean getMoreBlocks);

    List<? extends Block> popOffTo(int height);

    void registerDerivedTable(DerivedDbTable table);


    public static class BlockNotAcceptedException extends RiseException {

        BlockNotAcceptedException(String message) {
            super(message);
        }

    }

    public static class TransactionNotAcceptedException extends BlockNotAcceptedException {

        private final TransactionImpl transaction;

        TransactionNotAcceptedException(String message, TransactionImpl transaction) {
            super(message  + " transaction: " + transaction.getJSONObject().toJSONString());
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }

    }

    public static class BlockOutOfOrderException extends BlockNotAcceptedException {

        BlockOutOfOrderException(String message) {
            super(message);
        }

	}

}
