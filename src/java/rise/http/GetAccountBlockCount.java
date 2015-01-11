package rise.http;

import rise.Account;
import rise.Rise;
import rise.RiseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountBlockCount extends APIServlet.APIRequestHandler {

    static final GetAccountBlockCount instance = new GetAccountBlockCount();

    private GetAccountBlockCount() {
        super(new APITag[] {APITag.ACCOUNTS}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws RiseException {

        Account account = ParameterParser.getAccount(req);
        JSONObject response = new JSONObject();
        response.put("numberOfBlocks", Rise.getBlockchain().getBlockCount(account));

        return response;
    }

}
