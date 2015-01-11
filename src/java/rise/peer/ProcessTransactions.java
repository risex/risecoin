package rise.peer;

import rise.Rise;
import rise.RiseException;
import rise.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions extends PeerServlet.PeerRequestHandler {

    static final ProcessTransactions instance = new ProcessTransactions();

    private ProcessTransactions() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            Rise.getTransactionProcessor().processPeerTransactions(request);
            return JSON.emptyJSON;
        } catch (RuntimeException | RiseException.ValidationException e) {
            //Logger.logDebugMessage("Failed to parse peer transactions: " + request.toJSONString());
            peer.blacklist(e);
            JSONObject response = new JSONObject();
            response.put("error", e.toString());
            return response;
        }

    }

}
