package rise.http;

import rise.Account;
import rise.Attachment;
import rise.RiseException;
import rise.Order;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static rise.http.JSONResponses.UNKNOWN_ORDER;

public final class CancelAskOrder extends CreateTransaction {

    static final CancelAskOrder instance = new CancelAskOrder();

    private CancelAskOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "order");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws RiseException {
        long orderId = ParameterParser.getOrderId(req);
        Account account = ParameterParser.getSenderAccount(req);
        Order.Ask orderData = Order.Ask.getAskOrder(orderId);
        if (orderData == null || orderData.getAccountId() != account.getId()) {
            return UNKNOWN_ORDER;
        }
        Attachment attachment = new Attachment.ColoredCoinsAskOrderCancellation(orderId);
        return createTransaction(req, account, attachment);
    }

}
