package rise.http;

import rise.Alias;
import rise.RiseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAliasCount extends APIServlet.APIRequestHandler {

    static final GetAliasCount instance = new GetAliasCount();

    private GetAliasCount() {
        super(new APITag[] {APITag.ALIASES}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws RiseException {
        final long accountId = ParameterParser.getAccount(req).getId();
        JSONObject response = new JSONObject();
        response.put("numberOfAliases", Alias.getAccountAliasCount(accountId));
        return response;
    }

}
