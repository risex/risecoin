package rise.http;

import rise.RiseException;
import rise.Transaction;
import rise.util.Convert;
import rise.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ParseTransaction extends APIServlet.APIRequestHandler {

    static final ParseTransaction instance = new ParseTransaction();

    private ParseTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transactionBytes", "transactionJSON");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws RiseException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        Transaction transaction = ParameterParser.parseTransaction(transactionBytes, transactionJSON);
        JSONObject response = JSONData.unconfirmedTransaction(transaction);
        try {
            transaction.validate();
        } catch (RiseException.ValidationException|RuntimeException e) {
            Logger.logDebugMessage(e.getMessage(), e);
            response.put("validate", false);
            response.put("errorCode", 4);
            response.put("errorDescription", "Invalid transaction: " + e.toString());
            response.put("error", e.getMessage());
        }
        response.put("verify", transaction.verifySignature());
        return response;
    }

}
