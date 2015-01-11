package rise.http;

import rise.Account;
import rise.Attachment;
import rise.RiseException;
import rise.Poll;
import rise.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static rise.http.JSONResponses.INCORRECT_POLL;
import static rise.http.JSONResponses.INCORRECT_VOTE;
import static rise.http.JSONResponses.MISSING_POLL;

public final class CastVote extends CreateTransaction {

    static final CastVote instance = new CastVote();

    private CastVote() {
        super(new APITag[] {APITag.VS, APITag.CREATE_TRANSACTION}, "poll", "vote1", "vote2", "vote3"); // hardcoded to 3 votes for testing
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws RiseException {

        String pollValue = req.getParameter("poll");

        if (pollValue == null) {
            return MISSING_POLL;
        }

        Poll pollData;
        int numberOfOptions = 0;
        try {
            pollData = Poll.getPoll(Convert.parseUnsignedLong(pollValue));
            if (pollData != null) {
                numberOfOptions = pollData.getOptions().length;
            } else {
                return INCORRECT_POLL;
            }
        } catch (RuntimeException e) {
            return INCORRECT_POLL;
        }

        byte[] vote = new byte[numberOfOptions];
        try {
            for (int i = 0; i < numberOfOptions; i++) {
                String voteValue = req.getParameter("vote" + i);
                if (voteValue != null) {
                    vote[i] = Byte.parseByte(voteValue);
                }
            }
        } catch (NumberFormatException e) {
            return INCORRECT_VOTE;
        }

        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MessagingVoteCasting(pollData.getId(), vote);
        return createTransaction(req, account, attachment);

    }

}
