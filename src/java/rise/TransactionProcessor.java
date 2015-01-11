package rise;

import rise.db.DbIterator;
import rise.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface TransactionProcessor extends Observable<List<? extends Transaction>,TransactionProcessor.Event> {

    public static enum Event {
        REMOVED_UNCONFIRMED_TRANSACTIONS,
        ADDED_UNCONFIRMED_TRANSACTIONS,
        ADDED_CONFIRMED_TRANSACTIONS,
        ADDED_DOUBLESPENDING_TRANSACTIONS
    }

    DbIterator<? extends Transaction> getAllUnconfirmedTransactions();

    Transaction getUnconfirmedTransaction(long transactionId);

    void clearUnconfirmedTransactions();

    void broadcast(Transaction transaction) throws RiseException.ValidationException;

    void processPeerTransactions(JSONObject request) throws RiseException.ValidationException;

    Transaction parseTransaction(byte[] bytes) throws RiseException.ValidationException;

    Transaction parseTransaction(JSONObject json) throws RiseException.ValidationException;

    Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment);

}
