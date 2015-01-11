package rise.http;

import rise.Account;
import rise.Attachment;
import rise.Constants;
import rise.Rise;
import rise.RiseException;
import rise.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetRewardRecipient extends APIServlet.APIRequestHandler {
	
	static final GetRewardRecipient instance = new GetRewardRecipient();
	
	private GetRewardRecipient() {
		super(new APITag[] {APITag.ACCOUNTS, APITag.MINING, APITag.INFO}, "account");
	}
	
	@Override
    JSONStreamAware processRequest(HttpServletRequest req) throws RiseException {
		JSONObject response = new JSONObject();
		
		Account account = ParameterParser.getAccount(req);
		Account.RewardRecipientAssignment assignment = account.getRewardRecipientAssignment();
		long height = Rise.getBlockchain().getLastBlock().getHeight();
		if(account == null || assignment == null) {
			response.put("rewardRecipient", Convert.toUnsignedLong(account.getId()));
		}
		else if(assignment.getFromHeight() > height + 1) {
			response.put("rewardRecipient", Convert.toUnsignedLong(assignment.getPrevRecipientId()));
		}
		else {
			response.put("rewardRecipient", Convert.toUnsignedLong(assignment.getRecipientId()));
		}
		
		return response;
	}

}
