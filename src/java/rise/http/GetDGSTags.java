package rise.http;

import rise.DigitalGoodsStore;
import rise.RiseException;
import rise.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSTags extends APIServlet.APIRequestHandler {

    static final GetDGSTags instance = new GetDGSTags();

    private GetDGSTags() {
        super(new APITag[] {APITag.DGS, APITag.SEARCH}, "inStockOnly", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws RiseException {
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));

        JSONObject response = new JSONObject();
        JSONArray tagsJSON = new JSONArray();
        response.put("tags", tagsJSON);

        try (DbIterator<DigitalGoodsStore.Tag> tags = inStockOnly
                ? DigitalGoodsStore.getInStockTags(firstIndex, lastIndex) : DigitalGoodsStore.getAllTags(firstIndex, lastIndex)) {
            while (tags.hasNext()) {
                tagsJSON.add(JSONData.tag(tags.next()));
            }
        }
        return response;
    }

}
