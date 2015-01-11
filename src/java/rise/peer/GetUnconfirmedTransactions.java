package rise.peer;

import rise.Rise;
import rise.Transaction;
import rise.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetUnconfirmedTransactions extends PeerServlet.PeerRequestHandler {

    static final GetUnconfirmedTransactions instance = new GetUnconfirmedTransactions();

    private GetUnconfirmedTransactions() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        JSONArray transactionsData = new JSONArray();
        try (DbIterator<? extends Transaction> transactions = Rise.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            while (transactions.hasNext()) {
                Transaction transaction = transactions.next();
                transactionsData.add(transaction.getJSONObject());
            }
        }
        response.put("unconfirmedTransactions", transactionsData);


        return response;
    }

}
