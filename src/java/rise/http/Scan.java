package rise.http;

import rise.Rise;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class Scan extends APIServlet.APIRequestHandler {

    static final Scan instance = new Scan();

    private Scan() {
        super(new APITag[] {APITag.DEBUG}, "numBlocks", "height", "validate");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            if ("true".equalsIgnoreCase(req.getParameter("validate"))) {
                Rise.getBlockchainProcessor().validateAtNextScan();
            }
            int numBlocks = 0;
            try {
                numBlocks = Integer.parseInt(req.getParameter("numBlocks"));
            } catch (NumberFormatException e) {}
            int height = -1;
            try {
                height = Integer.parseInt(req.getParameter("height"));
            } catch (NumberFormatException ignore) {}
            long start = System.currentTimeMillis();
            try {
                if (numBlocks > 0) {
                    Rise.getBlockchainProcessor().setGetMoreBlocks(false);
                    Rise.getBlockchainProcessor().scan(Rise.getBlockchain().getHeight() - numBlocks + 1);
                } else if (height >= 0) {
                    Rise.getBlockchainProcessor().setGetMoreBlocks(false);
                    Rise.getBlockchainProcessor().scan(height);
                } else {
                    response.put("error", "invalid numBlocks or height");
                    return response;
                }
            } finally {
                Rise.getBlockchainProcessor().setGetMoreBlocks(true);
            }
            long end = System.currentTimeMillis();
            response.put("done", true);
            response.put("scanTime", (end - start)/1000);
        } catch (RuntimeException e) {
            response.put("error", e.toString());
        }
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

}
