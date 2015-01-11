package rise.http;

import rise.RiseException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSGood extends APIServlet.APIRequestHandler {

    static final GetDGSGood instance = new GetDGSGood();

    private GetDGSGood() {
        super(new APITag[] {APITag.DGS}, "goods", "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws RiseException {
        boolean includeCounts = !"false".equalsIgnoreCase(req.getParameter("includeCounts"));
        return JSONData.goods(ParameterParser.getGoods(req), includeCounts);
    }

}
