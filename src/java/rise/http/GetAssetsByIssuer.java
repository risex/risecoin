package rise.http;

import rise.Account;
import rise.Asset;
import rise.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAssetsByIssuer extends APIServlet.APIRequestHandler {

    static final GetAssetsByIssuer instance = new GetAssetsByIssuer();

    private GetAssetsByIssuer() {
        super(new APITag[] {APITag.AE, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex", "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        List<Account> accounts = ParameterParser.getAccounts(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeCounts = !"false".equalsIgnoreCase(req.getParameter("includeCounts"));

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        response.put("assets", accountsJSONArray);
        for (Account account : accounts) {
            JSONArray assetsJSONArray = new JSONArray();
            try (DbIterator<Asset> assets = Asset.getAssetsIssuedBy(account.getId(), firstIndex, lastIndex)) {
                while (assets.hasNext()) {
                    assetsJSONArray.add(JSONData.asset(assets.next(), includeCounts));
                }
            }
            accountsJSONArray.add(assetsJSONArray);
        }
        return response;
    }

}
