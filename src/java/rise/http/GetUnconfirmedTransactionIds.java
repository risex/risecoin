package rise.http;

import rise.Rise;
import rise.Transaction;
import rise.db.DbIterator;
import rise.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static rise.http.JSONResponses.INCORRECT_ACCOUNT;

public final class GetUnconfirmedTransactionIds extends APIServlet.APIRequestHandler {

    static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();

    private GetUnconfirmedTransactionIds() {
        super(new APITag[] {APITag.TRANSACTIONS, APITag.ACCOUNTS}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String accountIdString = Convert.emptyToNull(req.getParameter("account"));
        long accountId = 0;

        if (accountIdString != null) {
            try {
                accountId = Convert.parseAccountId(accountIdString);
            } catch (RuntimeException e) {
                return INCORRECT_ACCOUNT;
            }
        }

        JSONArray transactionIds = new JSONArray();
        try (DbIterator<? extends Transaction> transactionsIterator = Rise.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            while (transactionsIterator.hasNext()) {
                Transaction transaction = transactionsIterator.next();
                if (accountId != 0 && !(accountId == transaction.getSenderId() || accountId == transaction.getRecipientId())) {
                    continue;
                }
                transactionIds.add(transaction.getStringId());
            }
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactionIds", transactionIds);
        return response;
    }

}
